package no.kampklar.evm.service;

import no.kampklar.evm.model.Match;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Computes how each player's career caps/goals must change when a match is saved, as the
 * difference between the new and previously stored version of that match. Applying deltas
 * (rather than blindly incrementing) makes re-saving or correcting a result idempotent:
 * saving the same result twice changes nothing, and fixing a wrong scorer moves the goal.
 */
final class PlayerStatChanges {

    record Change(int caps, int goals) {
    }

    private PlayerStatChanges() {
    }

    /** Only players whose totals actually change are included. */
    static Map<String, Change> between(Match previous, Match updated) {
        Map<String, int[]> before = contributions(previous);
        Map<String, int[]> after = contributions(updated);

        Map<String, Change> changes = new HashMap<>();
        for (String playerId : union(before, after)) {
            int[] b = before.getOrDefault(playerId, new int[2]);
            int[] a = after.getOrDefault(playerId, new int[2]);
            Change change = new Change(a[0] - b[0], a[1] - b[1]);
            if (change.caps() != 0 || change.goals() != 0) {
                changes.put(playerId, change);
            }
        }
        return changes;
    }

    /** playerId -> {caps, goals} this match contributes; nothing unless it is finished. */
    private static Map<String, int[]> contributions(Match match) {
        Map<String, int[]> totals = new HashMap<>();
        if (match == null || !match.isFinished()) {
            return totals;
        }
        for (String playerId : concat(match.homeLineup(), match.awayLineup())) {
            totals.computeIfAbsent(playerId, id -> new int[2])[0]++;
        }
        for (String playerId : concat(match.homeScorerIds(), match.awayScorerIds())) {
            totals.computeIfAbsent(playerId, id -> new int[2])[1]++;
        }
        return totals;
    }

    private static List<String> concat(List<String> a, List<String> b) {
        return Stream.concat(a.stream(), b.stream()).toList();
    }

    private static Set<String> union(Map<String, ?> a, Map<String, ?> b) {
        Set<String> keys = new HashSet<>(a.keySet());
        keys.addAll(b.keySet());
        return keys;
    }
}
