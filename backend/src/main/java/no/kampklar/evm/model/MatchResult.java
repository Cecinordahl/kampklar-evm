package no.kampklar.evm.model;

/**
 * The minimal shape a group match needs to reduce to for standings computation -
 * deliberately narrower than the persisted Match entity (no date, scorers, competition id, ...)
 * so {@link no.kampklar.evm.service.UefaStandingsCalculator} stays a pure, easily testable function.
 */
public record MatchResult(String homeTeamId, int homeGoals, String awayTeamId, int awayGoals) {
}
