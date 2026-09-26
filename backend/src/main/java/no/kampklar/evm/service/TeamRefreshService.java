package no.kampklar.evm.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import no.kampklar.evm.model.Coach;
import no.kampklar.evm.model.Player;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static no.kampklar.evm.service.FirestoreDocuments.PLAYERS;
import static no.kampklar.evm.service.FirestoreDocuments.TEAMS;

/**
 * "Oppdater lagdata": researches a team and writes the result straight to Firestore - the
 * coach and tournament history on the team document, and the squad as player documents.
 *
 * <p>A batch rather than a transaction: research takes minutes, far too long to hold a
 * transaction open, and existing players only get name/position/club/inSquad written, never
 * caps/goals, so it cannot clobber a concurrent match entry.
 */
@Service
public class TeamRefreshService {

    public record Result(Coach coach, int tournamentsRecorded,
                         List<Player> playersAdded, List<Player> playersUpdated, List<Player> playersLeftSquad) {
    }

    private final Firestore firestore;
    private final TeamResearchService teamResearchService;

    // @Lazy: Firebase is not initialized when no service account is configured (e.g. `mvn test`).
    public TeamRefreshService(@Lazy Firestore firestore, TeamResearchService teamResearchService) {
        this.firestore = firestore;
        this.teamResearchService = teamResearchService;
    }

    public Result refresh(String teamId) {
        try {
            DocumentSnapshot teamDoc = firestore.collection(TEAMS).document(teamId).get().get();
            if (!teamDoc.exists()) {
                throw new NotFoundException("Team " + teamId + " not found");
            }

            TeamResearch research = teamResearchService.research(teamId, teamDoc.getString("name"));

            List<Player> existing = firestore.collection(PLAYERS).whereEqualTo("teamId", teamId).get().get()
                    .getDocuments().stream()
                    .map(FirestoreDocuments::toPlayer)
                    .toList();
            SquadMerge.Plan plan = SquadMerge.plan(teamId, existing, research.squad());

            WriteBatch batch = firestore.batch();
            batch.update(teamDoc.getReference(), Map.of(
                    "coach", FirestoreDocuments.fromCoach(research.coach()),
                    "tournamentHistory", FirestoreDocuments.fromTournamentHistory(research.tournamentHistory()),
                    "refreshedAt", FieldValue.serverTimestamp()));
            for (Player player : plan.added()) {
                batch.create(firestore.collection(PLAYERS).document(player.id()), FirestoreDocuments.fromPlayer(player));
            }
            for (Player player : plan.updated()) {
                batch.update(firestore.collection(PLAYERS).document(player.id()), Map.of(
                        "name", player.name(),
                        "position", player.position(),
                        "club", player.club(),
                        "inSquad", true));
            }
            for (Player player : plan.leftSquad()) {
                batch.update(firestore.collection(PLAYERS).document(player.id()), "inSquad", false);
            }
            batch.commit().get();

            return new Result(research.coach(), research.tournamentHistory().size(),
                    plan.added(), plan.updated(), plan.leftSquad());
        } catch (ExecutionException e) {
            throw new IllegalStateException("Refreshing team " + teamId + " failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while refreshing team " + teamId, e);
        }
    }
}
