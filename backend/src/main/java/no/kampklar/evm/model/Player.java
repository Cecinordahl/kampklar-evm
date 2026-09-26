package no.kampklar.evm.model;

/**
 * Firestore: {@code players/{id}}. Caps and goals are career totals for the national team.
 * A player who drops out of the squad keeps their document (old lineups reference it) with
 * {@code inSquad} set to false.
 */
public record Player(
        String id,
        String teamId,
        String name,
        String position,
        String club,
        int caps,
        int goals,
        boolean inSquad
) {
}
