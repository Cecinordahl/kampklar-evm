package no.kampklar.evm.dto;

import java.util.List;

/**
 * Body of PUT /admin/teams/{teamId}/details: the coach's descriptive fields and the data notes.
 * The coach's match record is not edited here and is kept as stored.
 */
public record TeamDetailsRequest(
        String coachName,
        String coachNationality,
        String coachBirthDate,
        String coachAppointedDate,
        String coachBio,
        List<String> dataNotes
) {
}
