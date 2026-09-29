package no.kampklar.evm.model;

/**
 * Embedded in {@link Team} - a team has exactly one head coach at a time. Dates are ISO
 * strings, since some are only known to the month (e.g. "2022-12"). Anything but the name
 * may be null (unknown).
 */
public record Coach(String name, String nationality, String birthDate, String appointedDate, String bio,
                    CoachRecord record) {

    public static Coach of(String name, String nationality) {
        return new Coach(name, nationality, null, null, null, null);
    }
}
