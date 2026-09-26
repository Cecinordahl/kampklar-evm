package no.kampklar.evm.dto;

import no.kampklar.evm.model.Coach;

import java.util.List;

/**
 * What the refresh changed. Refreshes save without a preview step, so the names are returned
 * for the admin to eyeball - a hallucinated or missing player shows up here immediately.
 */
public record TeamRefreshResponse(
        String teamId,
        Coach coach,
        int tournamentsRecorded,
        List<String> playersAdded,
        int playersUpdated,
        List<String> playersLeftSquad
) {
}
