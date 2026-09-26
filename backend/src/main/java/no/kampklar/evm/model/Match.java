package no.kampklar.evm.model;

import java.time.Instant;
import java.util.List;

/**
 * Firestore: {@code matches/{id}}. Goals and lineups are only meaningful once FINISHED.
 * Scorer lists repeat a player id once per goal (a brace appears twice); own goals count
 * toward the score but are not credited to any player, so a side's scorer list may be
 * shorter than its goal count.
 */
public record Match(
        String id,
        String competitionId,
        String groupId,
        String homeTeamId,
        String awayTeamId,
        Instant kickoff,
        MatchStatus status,
        Integer homeGoals,
        Integer awayGoals,
        List<String> homeLineup,
        List<String> awayLineup,
        List<String> homeScorerIds,
        List<String> awayScorerIds
) {

    public boolean isFinished() {
        return status == MatchStatus.FINISHED;
    }

    public MatchResult toResult() {
        return new MatchResult(homeTeamId, homeGoals, awayTeamId, awayGoals);
    }
}
