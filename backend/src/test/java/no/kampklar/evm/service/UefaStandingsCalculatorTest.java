package no.kampklar.evm.service;

import no.kampklar.evm.model.MatchResult;
import no.kampklar.evm.model.Standing;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UefaStandingsCalculatorTest {

    private final UefaStandingsCalculator calculator = new UefaStandingsCalculator();

    @Test
    void ranksTeamsByPointsWhenThereIsNoTie() {
        List<String> teams = List.of("norway", "spain", "france");
        List<MatchResult> matches = List.of(
                new MatchResult("norway", 2, "spain", 0),
                new MatchResult("norway", 1, "france", 0),
                new MatchResult("france", 2, "spain", 0)
        );

        List<Standing> table = calculator.compute(teams, matches);

        assertThat(table).extracting(Standing::teamId).containsExactly("norway", "france", "spain");
        assertThat(table).extracting(Standing::points).containsExactly(6, 3, 0);
    }

    @Test
    void headToHeadResultOutranksAFarBetterOverallGoalDifference() {
        // A beats B head-to-head, but B has thrashed the other two teams in the group while A
        // has lost to both - B's overall goal difference is +3, A's is -3. FIFA's overall-GD-
        // first order would rank B above A; UEFA's head-to-head-first order must rank A above B.
        List<String> teams = List.of("a", "b", "c", "d");
        List<MatchResult> matches = List.of(
                new MatchResult("a", 1, "b", 0),
                new MatchResult("c", 3, "a", 0),
                new MatchResult("d", 1, "a", 0),
                new MatchResult("b", 5, "c", 0),
                new MatchResult("d", 1, "b", 0),
                new MatchResult("c", 1, "d", 1)
        );

        List<Standing> table = calculator.compute(teams, matches);

        Standing a = findStanding(table, "a");
        Standing b = findStanding(table, "b");
        assertThat(a.points()).isEqualTo(3);
        assertThat(b.points()).isEqualTo(3);
        assertThat(a.goalDifference()).isEqualTo(-3);
        assertThat(b.goalDifference()).isEqualTo(3);

        assertThat(table).extracting(Standing::teamId).containsExactly("d", "c", "a", "b");
    }

    @Test
    void fallsThroughToOverallGoalDifferenceWhenHeadToHeadIsFullyTied() {
        // a, b, c all draw 0-0 with each other, so head-to-head comparison separates no one.
        // Their results against d give them equal points (5 each) but different overall goal
        // difference, which is what must decide the order.
        List<String> teams = List.of("a", "b", "c", "d");
        List<MatchResult> matches = List.of(
                new MatchResult("a", 0, "b", 0),
                new MatchResult("a", 0, "c", 0),
                new MatchResult("b", 0, "c", 0),
                new MatchResult("a", 2, "d", 0),
                new MatchResult("b", 4, "d", 0),
                new MatchResult("c", 1, "d", 0)
        );

        List<Standing> table = calculator.compute(teams, matches);

        assertThat(table).extracting(Standing::teamId).containsExactly("b", "a", "c", "d");
    }

    @Test
    void reappliesHeadToHeadExclusivelyToTeamsStillTiedAfterTheFirstPass() {
        // x, y, z are all tied 3 points apiece in a closed three-way group. The first
        // head-to-head pass (using all matches among the three) separates {x, y} - both on
        // goal difference 0 but goals-for 3, ahead of z on goals-for 2 - from z. x and y are
        // still exactly tied after that pass, so the second pass must reapply head-to-head
        // using only the x-vs-y match (y won it 3-1), not the three-team table.
        List<String> teams = List.of("x", "y", "z");
        List<MatchResult> matches = List.of(
                new MatchResult("x", 1, "y", 3),
                new MatchResult("y", 0, "z", 2),
                new MatchResult("z", 0, "x", 2)
        );

        List<Standing> table = calculator.compute(teams, matches);

        assertThat(table).extracting(Standing::points).containsExactly(3, 3, 3);
        assertThat(table).extracting(Standing::teamId).containsExactly("y", "x", "z");
    }

    @Test
    void fallsBackToAlphabeticalTeamIdWhenNoMatchesHaveBeenPlayed() {
        List<String> teams = List.of("spain", "france", "norway");

        List<Standing> table = calculator.compute(teams, List.of());

        assertThat(table).extracting(Standing::teamId).containsExactly("france", "norway", "spain");
        assertThat(table).allMatch(s -> s.points() == 0 && s.played() == 0);
    }

    private Standing findStanding(List<Standing> table, String teamId) {
        return table.stream().filter(s -> s.teamId().equals(teamId)).findFirst().orElseThrow();
    }
}
