package no.kampklar.evm.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import no.kampklar.evm.dto.ResultSuggestion;
import no.kampklar.evm.model.Group;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.Player;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static no.kampklar.evm.service.FirestoreDocuments.GROUPS;
import static no.kampklar.evm.service.FirestoreDocuments.MATCHES;
import static no.kampklar.evm.service.FirestoreDocuments.PLAYERS;
import static no.kampklar.evm.service.FirestoreDocuments.TEAMS;

/**
 * "Hent resultater med AI": researches every match in a group that has kicked off and returns
 * suggested results. Read-only - saving stays with {@link MatchService}, so the same validation
 * and standings recompute apply whether a result was typed in or suggested.
 */
@Service
public class ResultSuggestionService {

    private final Firestore firestore;
    private final ResultResearchService resultResearchService;

    // @Lazy: Firebase is not initialized when no service account is configured (e.g. `mvn test`).
    public ResultSuggestionService(@Lazy Firestore firestore, ResultResearchService resultResearchService) {
        this.firestore = firestore;
        this.resultResearchService = resultResearchService;
    }

    /** {@code matchIds} null or empty: every kicked-off match in the group. */
    public List<ResultSuggestion> suggest(String groupId, List<String> matchIds) {
        try {
            DocumentSnapshot groupDoc = firestore.collection(GROUPS).document(groupId).get().get();
            if (!groupDoc.exists()) {
                throw new NotFoundException("Group " + groupId + " not found");
            }
            Group group = FirestoreDocuments.toGroup(groupDoc);

            Instant now = Instant.now();
            List<Match> kickedOff = firestore.collection(MATCHES).whereEqualTo("groupId", groupId).get().get()
                    .getDocuments().stream()
                    .map(FirestoreDocuments::toMatch)
                    .filter(m -> m.kickoff().isBefore(now))
                    .filter(m -> matchIds == null || matchIds.isEmpty() || matchIds.contains(m.id()))
                    .toList();
            if (kickedOff.isEmpty()) {
                return List.of(); // nothing played yet - don't pay for a research call
            }

            Map<String, String> teamNames = new HashMap<>();
            Map<String, List<Player>> squads = new HashMap<>();
            for (String teamId : group.teamIds()) {
                teamNames.put(teamId, firestore.collection(TEAMS).document(teamId).get().get().getString("name"));
                squads.put(teamId, firestore.collection(PLAYERS)
                        .whereEqualTo("teamId", teamId).whereEqualTo("inSquad", true).get().get()
                        .getDocuments().stream().map(FirestoreDocuments::toPlayer).toList());
            }

            List<ResultResearchService.Fixture> fixtures = kickedOff.stream()
                    .map(m -> new ResultResearchService.Fixture(m.id(),
                            teamNames.get(m.homeTeamId()), teamNames.get(m.awayTeamId()), m.kickoff(),
                            names(squads.get(m.homeTeamId())), names(squads.get(m.awayTeamId()))))
                    .toList();
            ResultResearch research = resultResearchService.research(group.name(), fixtures);

            Map<String, Match> matchesById = kickedOff.stream().collect(Collectors.toMap(Match::id, m -> m));
            return research.results().stream()
                    .filter(ResultResearch.Result::played)
                    .filter(r -> matchesById.containsKey(r.matchId())) // ignore ids Claude made up
                    .map(r -> {
                        Match m = matchesById.get(r.matchId());
                        return ResultSuggestions.from(r, m,
                                teamNames.get(m.homeTeamId()), squads.get(m.homeTeamId()),
                                teamNames.get(m.awayTeamId()), squads.get(m.awayTeamId()));
                    })
                    .toList();
        } catch (ExecutionException e) {
            throw new IllegalStateException("Suggesting results for group " + groupId + " failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while suggesting results for group " + groupId, e);
        }
    }

    private static List<String> names(List<Player> squad) {
        return squad.stream().map(Player::name).toList();
    }
}
