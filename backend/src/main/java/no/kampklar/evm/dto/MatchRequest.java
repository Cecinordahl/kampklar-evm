package no.kampklar.evm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.MatchStatus;

import java.time.Instant;
import java.util.List;

/**
 * Body of PUT /admin/matches/{matchId}. Lineups and scorer lists may be omitted (e.g. when
 * only entering a fixture, or a score before the lineups are known); omitted means empty.
 */
public record MatchRequest(
        @NotBlank String competitionId,
        @NotBlank String groupId,
        @NotBlank String homeTeamId,
        @NotBlank String awayTeamId,
        @NotNull Instant kickoff,
        @NotNull MatchStatus status,
        @PositiveOrZero Integer homeGoals,
        @PositiveOrZero Integer awayGoals,
        List<@NotBlank String> homeLineup,
        List<@NotBlank String> awayLineup,
        List<@NotBlank String> homeScorerIds,
        List<@NotBlank String> awayScorerIds
) {

    public Match toMatch(String matchId) {
        return new Match(matchId, competitionId, groupId, homeTeamId, awayTeamId, kickoff, status,
                homeGoals, awayGoals,
                orEmpty(homeLineup), orEmpty(awayLineup), orEmpty(homeScorerIds), orEmpty(awayScorerIds));
    }

    private static List<String> orEmpty(List<String> list) {
        return list == null ? List.of() : list;
    }
}
