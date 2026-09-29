package no.kampklar.evm.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import no.kampklar.evm.model.Group;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.MatchResult;
import no.kampklar.evm.model.Player;
import no.kampklar.evm.model.Standing;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static no.kampklar.evm.service.FirestoreDocuments.GROUPS;
import static no.kampklar.evm.service.FirestoreDocuments.MATCHES;
import static no.kampklar.evm.service.FirestoreDocuments.PLAYERS;

/**
 * Creates or corrects a match and, in the same Firestore transaction, recomputes its group's
 * table and adjusts the affected players' career caps/goals. One transaction means the public
 * pages can never observe a result without its matching table, and concurrent edits to the
 * same group are retried by Firestore instead of overwriting each other's standings.
 */
@Service
public class MatchService {

    private final Firestore firestore;
    private final UefaStandingsCalculator standingsCalculator;

    // @Lazy: Firebase is not initialized when no service account is configured (e.g. `mvn test`).
    public MatchService(@Lazy Firestore firestore, UefaStandingsCalculator standingsCalculator) {
        this.firestore = firestore;
        this.standingsCalculator = standingsCalculator;
    }

    /** Saves the match and returns its group's recomputed table. */
    public List<Standing> save(Match match) {
        try {
            return firestore.runTransaction(tx -> saveInTransaction(tx, match)).get();
        } catch (ExecutionException e) {
            throw unwrap(e, match);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while saving match " + match.id(), e);
        }
    }

    // Firestore requires all reads to happen before any write within a transaction.
    private List<Standing> saveInTransaction(Transaction tx, Match match) throws Exception {
        DocumentReference matchRef = firestore.collection(MATCHES).document(match.id());
        DocumentReference groupRef = firestore.collection(GROUPS).document(match.groupId());

        DocumentSnapshot groupDoc = tx.get(groupRef).get();
        if (!groupDoc.exists()) {
            throw new NotFoundException("Group " + match.groupId() + " not found");
        }
        Group group = FirestoreDocuments.toGroup(groupDoc);
        DocumentSnapshot previousDoc = tx.get(matchRef).get();
        Match previous = previousDoc.exists() ? FirestoreDocuments.toMatch(previousDoc) : null;

        MatchValidator.validate(match, group, previous);

        Map<String, PlayerStatChanges.Change> statChanges = PlayerStatChanges.between(previous, match);
        Set<String> playerIds = new HashSet<>(statChanges.keySet());
        playerIds.addAll(match.homeLineup());
        playerIds.addAll(match.awayLineup());
        Map<String, Player> players = loadPlayers(tx, playerIds);
        MatchValidator.validatePlayers(match, players);

        List<Standing> standings = standingsCalculator.compute(group.teamIds(), finishedResultsInGroup(tx, match));

        tx.set(matchRef, FirestoreDocuments.fromMatch(match));
        tx.update(groupRef, Map.of(
                "standings", FirestoreDocuments.fromStandings(standings),
                "standingsUpdatedAt", FieldValue.serverTimestamp()));
        statChanges.forEach((playerId, change) -> {
            // A player removed from a lineup may since have been deleted - nothing to reverse then.
            Player player = players.get(playerId);
            if (player != null) {
                // Null means unknown: incrementing it would turn "unknown" into a made-up total.
                Map<String, Object> increments = new HashMap<>();
                if (player.caps() != null && change.caps() != 0) {
                    increments.put("caps", FieldValue.increment(change.caps()));
                }
                if (player.goals() != null && change.goals() != 0) {
                    increments.put("goals", FieldValue.increment(change.goals()));
                }
                if (!increments.isEmpty()) {
                    tx.update(firestore.collection(PLAYERS).document(playerId), increments);
                }
            }
        });
        return standings;
    }

    /** All finished results in the group, with the stored version of this match replaced by the new one. */
    private List<MatchResult> finishedResultsInGroup(Transaction tx, Match match) throws Exception {
        Stream<Match> otherMatches = tx.get(firestore.collection(MATCHES).whereEqualTo("groupId", match.groupId()))
                .get()
                .getDocuments()
                .stream()
                .map(FirestoreDocuments::toMatch)
                .filter(m -> !m.id().equals(match.id()));
        return Stream.concat(otherMatches, Stream.of(match))
                .filter(Match::isFinished)
                .map(Match::toResult)
                .toList();
    }

    private Map<String, Player> loadPlayers(Transaction tx, Set<String> playerIds) throws Exception {
        Map<String, Player> players = new LinkedHashMap<>();
        if (playerIds.isEmpty()) {
            return players;
        }
        DocumentReference[] refs = playerIds.stream()
                .map(id -> firestore.collection(PLAYERS).document(id))
                .toArray(DocumentReference[]::new);
        for (DocumentSnapshot doc : tx.getAll(refs).get()) {
            if (doc.exists()) {
                players.put(doc.getId(), FirestoreDocuments.toPlayer(doc));
            }
        }
        return players;
    }

    /** Surfaces our own 400/404 exceptions thrown inside the transaction instead of a generic 500. */
    private RuntimeException unwrap(ExecutionException e, Match match) {
        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof InvalidMatchException || cause instanceof NotFoundException) {
                return (RuntimeException) cause;
            }
        }
        return new IllegalStateException("Saving match " + match.id() + " failed", e.getCause());
    }
}
