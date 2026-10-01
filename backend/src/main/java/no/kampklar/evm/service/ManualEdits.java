package no.kampklar.evm.service;

import no.kampklar.evm.dto.PlayerEditRequest;
import no.kampklar.evm.dto.TeamDetailsRequest;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates manual edits and turns them into the Firestore fields to write. Pure functions, so
 * the rules are unit-tested without Firestore. Blank text is stored as null (unknown).
 */
final class ManualEdits {

    private static final Set<String> POSITIONS = Set.of("GK", "DF", "MF", "FW");
    private static final Set<String> SQUAD_STATUSES = Set.of("confirmed", "considered");

    private ManualEdits() {
    }

    static Map<String, Object> playerFields(PlayerEditRequest r) {
        String position = blankToNull(r.position());
        if (position == null || !POSITIONS.contains(position)) {
            throw new InvalidEditException("Posisjon må være GK, DF, MF eller FW");
        }
        String squadStatus = blankToNull(r.squadStatus());
        if (squadStatus != null && !SQUAD_STATUSES.contains(squadStatus)) {
            throw new InvalidEditException("Troppstatus må være confirmed, considered eller tom");
        }
        String birthDate = isoDate(r.birthDate(), "Fødselsdato");
        Integer birthYear = birthDate != null ? Integer.valueOf(LocalDate.parse(birthDate).getYear()) : r.birthYear();
        if (birthYear != null && (birthYear < 1900 || birthYear > Year.now().getValue())) {
            throw new InvalidEditException("Fødselsår " + birthYear + " er ikke gyldig");
        }
        if ((r.caps() != null && r.caps() < 0) || (r.goals() != null && r.goals() < 0)) {
            throw new InvalidEditException("Landskamper og mål kan ikke være negative");
        }

        Map<String, Object> fields = new HashMap<>();
        fields.put("club", blankToNull(r.club()));
        fields.put("position", position);
        fields.put("birthDate", birthDate);
        fields.put("birthYear", birthYear);
        // A manually entered date or year is a verified value.
        fields.put("birthYearUnverified", false);
        fields.put("squadStatus", squadStatus);
        // inSquad follows the status, so the squad list and the "considered" section stay consistent.
        fields.put("inSquad", "confirmed".equals(squadStatus));
        fields.put("captain", r.captain());
        fields.put("note", blankToNull(r.note()));
        fields.put("caps", r.caps());
        fields.put("goals", r.goals());
        return fields;
    }

    /** Dotted paths, so the coach's stored match record is left as it is. */
    static Map<String, Object> teamDetailFields(TeamDetailsRequest r) {
        String coachName = blankToNull(r.coachName());
        if (coachName == null) {
            throw new InvalidEditException("Landslagssjef må ha et navn");
        }
        Map<String, Object> fields = new HashMap<>();
        fields.put("coach.name", coachName);
        fields.put("coach.nationality", blankToNull(r.coachNationality()));
        fields.put("coach.birthDate", isoDate(r.coachBirthDate(), "Landslagssjefens fødselsdato"));
        fields.put("coach.appointedDate", isoDateOrMonth(r.coachAppointedDate()));
        fields.put("coach.bio", blankToNull(r.coachBio()));
        fields.put("dataNotes", r.dataNotes() == null ? List.of()
                : r.dataNotes().stream().map(ManualEdits::blankToNull).filter(n -> n != null).toList());
        return fields;
    }

    private static String isoDate(String value, String label) {
        String date = blankToNull(value);
        if (date == null) {
            return null;
        }
        try {
            LocalDate.parse(date);
            return date;
        } catch (DateTimeParseException e) {
            throw new InvalidEditException(label + " må være på formen ÅÅÅÅ-MM-DD");
        }
    }

    /** Appointment dates are sometimes only known to the month ("2022-12"). */
    private static String isoDateOrMonth(String value) {
        String date = blankToNull(value);
        if (date == null) {
            return null;
        }
        try {
            if (date.length() == 7) {
                YearMonth.parse(date);
            } else {
                LocalDate.parse(date);
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new InvalidEditException("Ansettelsesdato må være ÅÅÅÅ-MM-DD eller ÅÅÅÅ-MM");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
