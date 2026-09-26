package no.kampklar.evm.service;

import no.kampklar.evm.model.Group;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.MatchStatus;
import no.kampklar.evm.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static no.kampklar.evm.service.MatchFixtures.KICKOFF;
import static no.kampklar.evm.service.MatchFixtures.finished;
import static no.kampklar.evm.service.MatchFixtures.scheduled;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchValidatorTest {

    private final Group group = new Group("a1", "unl", "league-a", "A1",
            List.of("norway", "spain", "france", "italy"), List.of());

    @Test
    void acceptsAValidFinishedMatch() {
        Match match = finished(2, 1, List.of("haaland"), List.of("yamal"), List.of("haaland"), List.of("yamal"));

        assertThatCode(() -> MatchValidator.validate(match, group, null)).doesNotThrowAnyException();
    }

    @Test
    void acceptsAFinishedMatchWithoutLineupsYet() {
        Match match = finished(3, 0, List.of(), List.of(), List.of(), List.of());

        assertThatCode(() -> MatchValidator.validate(match, group, null)).doesNotThrowAnyException();
    }

    @Test
    void rejectsATeamOutsideTheGroup() {
        Match match = new Match("m1", "unl", "a1", "norway", "germany", KICKOFF, MatchStatus.SCHEDULED,
                null, null, List.of(), List.of(), List.of(), List.of());

        assertThatThrownBy(() -> MatchValidator.validate(match, group, null))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessageContaining("group a1");
    }

    @Test
    void rejectsMovingAnExistingMatchToAnotherGroup() {
        Match previous = new Match("m1", "unl", "a2", "norway", "spain", KICKOFF, MatchStatus.SCHEDULED,
                null, null, List.of(), List.of(), List.of(), List.of());

        assertThatThrownBy(() -> MatchValidator.validate(scheduled(), group, previous))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessageContaining("another group");
    }

    @Test
    void rejectsAFinishedMatchWithoutScores() {
        Match match = finished(1, 0, List.of(), List.of(), List.of(), List.of());
        Match missingScore = new Match(match.id(), match.competitionId(), match.groupId(), match.homeTeamId(),
                match.awayTeamId(), KICKOFF, MatchStatus.FINISHED, 1, null,
                List.of(), List.of(), List.of(), List.of());

        assertThatThrownBy(() -> MatchValidator.validate(missingScore, group, null))
                .isInstanceOf(InvalidMatchException.class);
    }

    @Test
    void rejectsMoreScorersThanGoals() {
        Match match = finished(1, 0, List.of("haaland"), List.of(), List.of("haaland", "haaland"), List.of());

        assertThatThrownBy(() -> MatchValidator.validate(match, group, null))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessageContaining("scorers");
    }

    @Test
    void rejectsAScorerWhoWasNotInTheLineup() {
        Match match = finished(1, 0, List.of("odegaard"), List.of(), List.of("haaland"), List.of());

        assertThatThrownBy(() -> MatchValidator.validate(match, group, null))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessageContaining("haaland");
    }

    @Test
    void rejectsAScheduledMatchCarryingAScore() {
        Match match = new Match("m1", "unl", "a1", "norway", "spain", KICKOFF, MatchStatus.SCHEDULED,
                1, 0, List.of(), List.of(), List.of(), List.of());

        assertThatThrownBy(() -> MatchValidator.validate(match, group, null))
                .isInstanceOf(InvalidMatchException.class);
    }

    @Test
    void rejectsALineupPlayerFromTheWrongTeam() {
        Match match = finished(0, 0, List.of("yamal"), List.of(), List.of(), List.of());
        Map<String, Player> players = Map.of("yamal", new Player("yamal", "spain", "Lamine Yamal", "FW", 20, 5));

        assertThatThrownBy(() -> MatchValidator.validatePlayers(match, players))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessageContaining("does not play for norway");
    }

    @Test
    void rejectsAnUnknownLineupPlayer() {
        Match match = finished(0, 0, List.of("nobody"), List.of(), List.of(), List.of());

        assertThatThrownBy(() -> MatchValidator.validatePlayers(match, Map.of()))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessageContaining("Unknown player");
    }
}
