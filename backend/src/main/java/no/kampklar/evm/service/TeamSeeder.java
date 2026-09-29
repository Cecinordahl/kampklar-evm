package no.kampklar.evm.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteBatch;
import no.kampklar.evm.model.Coach;
import no.kampklar.evm.model.CoachRecord;
import no.kampklar.evm.model.Player;
import no.kampklar.evm.model.Team;
import no.kampklar.evm.model.TournamentHistoryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static no.kampklar.evm.service.FirestoreDocuments.PLAYERS;
import static no.kampklar.evm.service.FirestoreDocuments.TEAMS;

/**
 * Loads the hand-researched squads, coaches and tournament history for the featured teams from
 * {@code seed/kampklar-seed-teams.json}. Runs alongside {@link FixtureSeeder} under the "seed"
 * profile.
 *
 * <p>Safe to re-run: players are matched to existing documents by normalized name (the same key
 * "Oppdater lagdata" uses), so nothing is duplicated. Caps/goals are a baseline and are only
 * written when a player is created - after that match entry owns them. Everything else (club,
 * notes, coach, history, "sist oppdatert") is reset to the seed file's values.
 */
@Component
@Profile("seed")
class TeamSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TeamSeeder.class);
    static final String SEED_FILE = "seed/kampklar-seed-teams.json";
    private static final ZoneId OSLO = ZoneId.of("Europe/Oslo");

    /** The seed file keys teams by short code; our documents use the slug from unl-2026-27.json. */
    static final Map<String, String> TEAM_IDS_BY_FIFA_CODE = Map.of(
            "NOR", "norway",
            "ESP", "spain",
            "FRA", "france");

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedFile(Map<String, SeedTeam> teams) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedTeam(String name, String fifaCode, String flagCountryCode, String currentCompetition,
                    String statsAsOf, String lastRefreshedAt, List<String> dataNotes, SeedCoach coach,
                    List<SeedPlayer> players, List<SeedTournament> tournamentHistory) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedCoach(String name, String birthDate, String appointedDate, CoachRecord record, String bio) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedPlayer(String name, String position, String club, String birthDate, Integer birthYear,
                      Boolean birthYearUnverified, Integer caps, Integer goals, String squadStatus,
                      Boolean isCaptain, String note) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedTournament(String competition, int year, String result, String detail) {
    }

    private final Firestore firestore;
    private final JsonMapper jsonMapper;

    TeamSeeder(Firestore firestore, JsonMapper jsonMapper) {
        this.firestore = firestore;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        SeedFile seed;
        try (InputStream in = new ClassPathResource(SEED_FILE).getInputStream()) {
            seed = jsonMapper.readValue(in, SeedFile.class);
        }
        for (SeedTeam seedTeam : seed.teams().values()) {
            seedTeam(seedTeam);
        }
    }

    private void seedTeam(SeedTeam seedTeam) throws Exception {
        String teamId = teamId(seedTeam);
        Map<String, Player> existingByName = new HashMap<>();
        Set<String> takenIds = new HashSet<>();
        firestore.collection(PLAYERS).whereEqualTo("teamId", teamId).get().get().getDocuments().forEach(doc -> {
            Player player = FirestoreDocuments.toPlayer(doc);
            existingByName.put(SquadMerge.normalize(player.name()), player);
            takenIds.add(player.id());
        });

        WriteBatch batch = firestore.batch();
        // Merge, so fields the seed file does not know about (e.g. standings references) survive.
        batch.set(firestore.collection(TEAMS).document(teamId), FirestoreDocuments.fromTeam(toTeam(teamId, seedTeam)),
                SetOptions.merge());

        int created = 0;
        List<String> baselineMismatches = new ArrayList<>();
        for (SeedPlayer seedPlayer : seedTeam.players()) {
            String key = SquadMerge.normalize(seedPlayer.name());
            Player existing = existingByName.get(key);
            String id = existing != null ? existing.id() : SquadMerge.uniqueId(teamId + "-" + key, takenIds);
            Player player = toPlayer(id, teamId, seedPlayer);
            DocumentReference ref = firestore.collection(PLAYERS).document(id);
            if (existing == null) {
                batch.create(ref, FirestoreDocuments.fromPlayer(player));
                created++;
            } else {
                batch.set(ref, FirestoreDocuments.fromPlayerExceptStats(player), SetOptions.merge());
                if (!Objects.equals(existing.caps(), player.caps()) || !Objects.equals(existing.goals(), player.goals())) {
                    baselineMismatches.add("%s (stored %s/%s, seed %s/%s)".formatted(player.name(),
                            existing.caps(), existing.goals(), player.caps(), player.goals()));
                }
            }
        }
        batch.commit().get();

        log.info("Seeded team {}: {} players ({} new, {} updated)", teamId, seedTeam.players().size(),
                created, seedTeam.players().size() - created);
        if (!baselineMismatches.isEmpty()) {
            // Expected once results are entered; otherwise an earlier refresh wrote other figures.
            log.warn("Kept stored caps/goals for {} player(s) on {} that differ from the seed baseline: {}",
                    baselineMismatches.size(), teamId, baselineMismatches);
        }
    }

    static String teamId(SeedTeam seedTeam) {
        String teamId = TEAM_IDS_BY_FIFA_CODE.get(seedTeam.fifaCode());
        if (teamId == null) {
            throw new IllegalStateException("No team id for FIFA code " + seedTeam.fifaCode());
        }
        return teamId;
    }

    static Team toTeam(String teamId, SeedTeam t) {
        SeedCoach c = t.coach();
        // The seed file has no coach nationality; unknown rather than guessed.
        Coach coach = new Coach(c.name(), null, c.birthDate(), c.appointedDate(), c.bio(), c.record());
        List<TournamentHistoryEntry> history = t.tournamentHistory().stream()
                .map(h -> new TournamentHistoryEntry(h.competition(), h.year(), h.result(), h.detail()))
                .toList();
        return new Team(teamId, t.name(), t.fifaCode(), t.flagCountryCode(), t.currentCompetition(), coach, history,
                t.statsAsOf(), t.dataNotes(), LocalDate.parse(t.lastRefreshedAt()).atStartOfDay(OSLO).toInstant());
    }

    static Player toPlayer(String id, String teamId, SeedPlayer p) {
        boolean confirmed = "confirmed".equals(p.squadStatus());
        return new Player(id, teamId, p.name(), p.position(), p.club(), p.birthDate(), p.birthYear(),
                Boolean.TRUE.equals(p.birthYearUnverified()), p.caps(), p.goals(), confirmed, p.squadStatus(),
                Boolean.TRUE.equals(p.isCaptain()), p.note());
    }
}
