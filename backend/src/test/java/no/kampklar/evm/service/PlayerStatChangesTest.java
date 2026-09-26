package no.kampklar.evm.service;

import no.kampklar.evm.model.Match;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static no.kampklar.evm.service.MatchFixtures.finished;
import static no.kampklar.evm.service.MatchFixtures.scheduled;
import static org.assertj.core.api.Assertions.assertThat;

class PlayerStatChangesTest {

    @Test
    void firstResultGivesEveryLineupPlayerACapAndEachScorerTheirGoals() {
        Match result = finished(2, 1, List.of("haaland", "odegaard"), List.of("yamal"),
                List.of("haaland", "haaland"), List.of("yamal"));

        Map<String, PlayerStatChanges.Change> changes = PlayerStatChanges.between(null, result);

        assertThat(changes).containsExactlyInAnyOrderEntriesOf(Map.of(
                "haaland", new PlayerStatChanges.Change(1, 2),
                "odegaard", new PlayerStatChanges.Change(1, 0),
                "yamal", new PlayerStatChanges.Change(1, 1)));
    }

    @Test
    void resavingTheSameResultChangesNothing() {
        Match result = finished(1, 0, List.of("haaland"), List.of("yamal"), List.of("haaland"), List.of());

        assertThat(PlayerStatChanges.between(result, result)).isEmpty();
    }

    @Test
    void correctingTheScorerMovesTheGoalWithoutTouchingCaps() {
        Match wrong = finished(1, 0, List.of("haaland", "odegaard"), List.of(), List.of("haaland"), List.of());
        Match corrected = finished(1, 0, List.of("haaland", "odegaard"), List.of(), List.of("odegaard"), List.of());

        assertThat(PlayerStatChanges.between(wrong, corrected)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "haaland", new PlayerStatChanges.Change(0, -1),
                "odegaard", new PlayerStatChanges.Change(0, 1)));
    }

    @Test
    void removingAPlayerFromTheLineupTakesBackTheirCap() {
        Match before = finished(0, 0, List.of("haaland", "odegaard"), List.of(), List.of(), List.of());
        Match after = finished(0, 0, List.of("haaland"), List.of(), List.of(), List.of());

        assertThat(PlayerStatChanges.between(before, after))
                .containsExactlyEntriesOf(Map.of("odegaard", new PlayerStatChanges.Change(-1, 0)));
    }

    @Test
    void revertingAFinishedMatchToScheduledReversesAllItsStats() {
        Match result = finished(1, 0, List.of("haaland"), List.of(), List.of("haaland"), List.of());

        assertThat(PlayerStatChanges.between(result, scheduled()))
                .containsExactlyEntriesOf(Map.of("haaland", new PlayerStatChanges.Change(-1, -1)));
    }
}
