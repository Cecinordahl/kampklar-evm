package no.kampklar.evm.dto;

import java.util.List;

/**
 * A researched result for one match, with players already mapped to our ids. Nothing is saved:
 * the admin reviews it in the match form and saves through PUT /admin/matches/{id} as usual.
 * {@code warnings} lists what could not be mapped (e.g. a player missing from the stored squad).
 */
public record ResultSuggestion(
        String matchId,
        int homeGoals,
        int awayGoals,
        List<String> homeLineup,
        List<String> awayLineup,
        List<String> homeScorerIds,
        List<String> awayScorerIds,
        List<String> warnings
) {
}
