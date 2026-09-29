package no.kampklar.evm.model;

import java.time.Instant;
import java.util.List;

/**
 * Firestore: {@code teams/{id}}, where id is the stable slug also used on the frontend
 * (e.g. "norway"). Players live in their own collection so caps/goals can be incremented
 * per player without rewriting the whole team document.
 *
 * <p>{@code statsAsOf} (ISO date) is when the stored caps/goals baseline was published;
 * {@code dataNotes} are caveats about the data shown alongside it; {@code refreshedAt} drives
 * the "sist oppdatert" label.
 */
public record Team(
        String id,
        String name,
        String fifaCode,
        String flagCountryCode,
        String currentCompetition,
        Coach coach,
        List<TournamentHistoryEntry> tournamentHistory,
        String statsAsOf,
        List<String> dataNotes,
        Instant refreshedAt
) {
}
