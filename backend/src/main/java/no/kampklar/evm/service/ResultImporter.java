package no.kampklar.evm.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import no.kampklar.evm.dto.ResultSuggestion;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.MatchStatus;
import no.kampklar.evm.model.Player;
import no.kampklar.evm.model.Standing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.kampklar.evm.service.FirestoreDocuments.MATCHES;
import static no.kampklar.evm.service.FirestoreDocuments.PLAYERS;
import static no.kampklar.evm.service.FirestoreDocuments.TEAMS;

/**
 * Saves already-researched results from a JSON file without going through the admin UI:
 * {@code --spring.profiles.active=import-results --results=path/to/results.json}.
 *
 * <p>Player names are mapped to ids exactly like "Hent resultater med AI", and every match is
 * saved through {@link MatchService}, so the group table and caps/goals update as with manual
 * entry. All matches are checked before any is saved: an unknown player for a team that has a
 * stored squad aborts the whole import. Re-running is safe (caps/goals are applied as deltas).
 */
@Component
@Profile("import-results")
class ResultImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ResultImporter.class);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResultFile(List<ResultResearch.Result> results) {
    }

    private final Firestore firestore;
    private final JsonMapper jsonMapper;
    private final MatchService matchService;

    ResultImporter(Firestore firestore, JsonMapper jsonMapper, MatchService matchService) {
        this.firestore = firestore;
        this.jsonMapper = jsonMapper;
        this.matchService = matchService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> paths = args.getOptionValues("results");
        if (paths == null || paths.size() != 1) {
            throw new IllegalArgumentException("Pass the file to import as --results=path/to/results.json");
        }
        ResultFile file = jsonMapper.readValue(Path.of(paths.getFirst()).toFile(), ResultFile.class);

        List<Match> toSave = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        Map<String, List<Player>> squads = new HashMap<>();
        for (ResultResearch.Result result : file.results()) {
            DocumentSnapshot doc = firestore.collection(MATCHES).document(result.matchId()).get().get();
            if (!doc.exists()) {
                problems.add("Unknown match " + result.matchId());
                continue;
            }
            Match stored = FirestoreDocuments.toMatch(doc);
            List<Player> homeSquad = squads.computeIfAbsent(stored.homeTeamId(), this::players);
            List<Player> awaySquad = squads.computeIfAbsent(stored.awayTeamId(), this::players);
            ResultSuggestion mapped = ResultSuggestions.from(result, stored,
                    teamName(stored.homeTeamId()), homeSquad, teamName(stored.awayTeamId()), awaySquad);
            // A team without a stored squad gets the score only - that is expected, not an error.
            mapped.warnings().stream()
                    .filter(w -> !w.startsWith("Ingen tropp lagret"))
                    .forEach(w -> problems.add(result.matchId() + ": " + w));
            toSave.add(new Match(stored.id(), stored.competitionId(), stored.groupId(), stored.homeTeamId(),
                    stored.awayTeamId(), stored.kickoff(), MatchStatus.FINISHED,
                    mapped.homeGoals(), mapped.awayGoals(), mapped.homeLineup(), mapped.awayLineup(),
                    mapped.homeScorerIds(), mapped.awayScorerIds()));
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Nothing saved - fix these first:\n  " + String.join("\n  ", problems));
        }

        List<Standing> standings = List.of();
        for (Match match : toSave) {
            standings = matchService.save(match);
            log.info("Saved {} {}-{} (lineups {}+{}, scorers {}+{})", match.id(), match.homeGoals(), match.awayGoals(),
                    match.homeLineup().size(), match.awayLineup().size(),
                    match.homeScorerIds().size(), match.awayScorerIds().size());
        }
        standings.forEach(s -> log.info("  {}. {} {}p ({}-{})", s.rank(), s.teamId(), s.points(),
                s.goalsFor(), s.goalsAgainst()));
    }

    private List<Player> players(String teamId) {
        try {
            return firestore.collection(PLAYERS).whereEqualTo("teamId", teamId).get().get().getDocuments().stream()
                    .map(FirestoreDocuments::toPlayer)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Loading players for " + teamId + " failed", e);
        }
    }

    private String teamName(String teamId) {
        try {
            String name = firestore.collection(TEAMS).document(teamId).get().get().getString("name");
            return name == null ? teamId : name;
        } catch (Exception e) {
            throw new IllegalStateException("Loading team " + teamId + " failed", e);
        }
    }
}
