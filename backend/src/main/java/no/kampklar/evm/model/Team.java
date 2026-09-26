package no.kampklar.evm.model;

import java.util.List;

/**
 * Firestore: {@code teams/{id}}, where id is the stable slug also used on the frontend
 * (e.g. "norway"). Players live in their own collection so caps/goals can be incremented
 * per player without rewriting the whole team document.
 */
public record Team(String id, String name, Coach coach, List<TournamentHistoryEntry> tournamentHistory) {
}
