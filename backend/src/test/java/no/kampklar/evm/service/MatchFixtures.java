package no.kampklar.evm.service;

import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.MatchStatus;

import java.time.Instant;
import java.util.List;

final class MatchFixtures {

    static final Instant KICKOFF = Instant.parse("2026-09-24T18:45:00Z");

    private MatchFixtures() {
    }

    static Match scheduled() {
        return new Match("m1", "unl", "a1", "norway", "spain", KICKOFF, MatchStatus.SCHEDULED,
                null, null, List.of(), List.of(), List.of(), List.of());
    }

    static Match finished(int homeGoals, int awayGoals, List<String> homeLineup, List<String> awayLineup,
                          List<String> homeScorerIds, List<String> awayScorerIds) {
        return new Match("m1", "unl", "a1", "norway", "spain", KICKOFF, MatchStatus.FINISHED,
                homeGoals, awayGoals, homeLineup, awayLineup, homeScorerIds, awayScorerIds);
    }
}
