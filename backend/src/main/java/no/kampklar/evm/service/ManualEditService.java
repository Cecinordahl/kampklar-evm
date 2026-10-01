package no.kampklar.evm.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import no.kampklar.evm.dto.PlayerEditRequest;
import no.kampklar.evm.dto.TeamDetailsRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static no.kampklar.evm.service.FirestoreDocuments.PLAYERS;
import static no.kampklar.evm.service.FirestoreDocuments.TEAMS;

/** Admin mode's manual corrections to a player or a team's descriptive data. */
@Service
public class ManualEditService {

    private final Firestore firestore;

    // @Lazy: Firebase is not initialized when no service account is configured (e.g. `mvn test`).
    public ManualEditService(@Lazy Firestore firestore) {
        this.firestore = firestore;
    }

    public void editPlayer(String playerId, PlayerEditRequest request) {
        update(firestore.collection(PLAYERS).document(playerId), ManualEdits.playerFields(request), "Player " + playerId);
    }

    public void editTeamDetails(String teamId, TeamDetailsRequest request) {
        update(firestore.collection(TEAMS).document(teamId), ManualEdits.teamDetailFields(request), "Team " + teamId);
    }

    // update() rather than set(): it fails on a missing document instead of creating a stray one.
    private void update(DocumentReference ref, Map<String, Object> fields, String label) {
        try {
            if (!ref.get().get().exists()) {
                throw new NotFoundException(label + " not found");
            }
            ref.update(fields).get();
        } catch (ExecutionException e) {
            throw new IllegalStateException("Saving " + label + " failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while saving " + label, e);
        }
    }
}
