package no.kampklar.evm.model;

/**
 * Firestore: {@code players/{id}}. Caps and goals are career totals for the national team;
 * null means unknown (not zero) and stays null through match entry. A player who drops out of
 * the squad keeps their document (old lineups reference it) with {@code inSquad} set to false.
 *
 * <p>Age is never stored: {@code birthDate} (ISO yyyy-MM-dd) or, when only the year is known,
 * {@code birthYear}, and the frontend computes the age. {@code squadStatus} is "confirmed"
 * (in the current squad), "considered" (named as considered, or withdrawn injured) or null.
 */
public record Player(
        String id,
        String teamId,
        String name,
        String position,
        String club,
        String birthDate,
        Integer birthYear,
        boolean birthYearUnverified,
        Integer caps,
        Integer goals,
        boolean inSquad,
        String squadStatus,
        boolean captain,
        String note
) {

    /** A player as found by research: only the basics are known. */
    public static Player researched(String id, String teamId, String name, String position, String club,
                                    Integer caps, Integer goals, boolean inSquad) {
        return new Player(id, teamId, name, position, club, null, null, false, caps, goals, inSquad,
                inSquad ? "confirmed" : null, false, null);
    }
}
