package no.kampklar.evm.service;

import no.kampklar.evm.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class SquadMergeTest {

    private static final Player HAALAND = new Player("haaland", "norway", "Erling Haaland", "FW", "Man City", 45, 42, true);
    private static final Player ODEGAARD = new Player("odegaard", "norway", "Martin Ødegaard", "MF", "Arsenal", 60, 5, true);

    @Test
    void addsANewPlayerWithResearchedCapsAndGoalsAndASlugId() {
        SquadMerge.Plan plan = SquadMerge.plan("norway", List.of(),
                List.of(new TeamResearch.Player("Antonio Nusa", "FW", "RB Leipzig", 15, 3)));

        assertThat(plan.added()).containsExactly(
                new Player("norway-antonio-nusa", "norway", "Antonio Nusa", "FW", "RB Leipzig", 15, 3, true));
        assertThat(plan.updated()).isEmpty();
    }

    @Test
    void keepsStoredCapsAndGoalsForAnExistingPlayerButTakesTheirNewClub() {
        SquadMerge.Plan plan = SquadMerge.plan("norway", List.of(HAALAND),
                List.of(new TeamResearch.Player("Erling Haaland", "FW", "Real Madrid", 44, 40)));

        assertThat(plan.updated()).containsExactly(
                new Player("haaland", "norway", "Erling Haaland", "FW", "Real Madrid", 45, 42, true));
        assertThat(plan.added()).isEmpty();
    }

    @Test
    void matchesAPlayerWhoseNameIsSpelledWithoutDiacritics() {
        SquadMerge.Plan plan = SquadMerge.plan("norway", List.of(ODEGAARD),
                List.of(new TeamResearch.Player("Martin Odegaard", "MF", "Arsenal", 61, 5)));

        assertThat(plan.updated()).extracting(Player::id).containsExactly("odegaard");
        assertThat(plan.added()).isEmpty();
    }

    @Test
    void marksAPlayerMissingFromTheNewSquadAsLeftRatherThanDeletingThem() {
        SquadMerge.Plan plan = SquadMerge.plan("norway", List.of(HAALAND, ODEGAARD),
                List.of(new TeamResearch.Player("Erling Haaland", "FW", "Man City", 45, 42)));

        assertThat(plan.leftSquad()).extracting(Player::id, Player::inSquad).containsExactly(
                tuple("odegaard", false));
    }

    @Test
    void doesNotReportAPlayerWhoAlreadyLeftAsLeavingAgain() {
        Player alreadyGone = new Player("riise", "norway", "John Arne Riise", "DF", null, 110, 16, false);

        SquadMerge.Plan plan = SquadMerge.plan("norway", List.of(alreadyGone), List.of());

        assertThat(plan.leftSquad()).isEmpty();
    }

    @Test
    void ignoresAPlayerListedTwiceInTheResearch() {
        SquadMerge.Plan plan = SquadMerge.plan("norway", List.of(), List.of(
                new TeamResearch.Player("Sander Berge", "MF", "Fulham", 50, 1),
                new TeamResearch.Player("Sander Berge", "MF", "Fulham", 50, 1)));

        assertThat(plan.added()).hasSize(1);
    }

    @Test
    void avoidsAnIdCollisionWithADifferentExistingPlayer() {
        Player sameSlugDifferentName = new Player("norway-ola-aina", "norway", "Ola Aina Sr", "DF", null, 3, 0, true);

        SquadMerge.Plan plan = SquadMerge.plan("norway", List.of(sameSlugDifferentName),
                List.of(new TeamResearch.Player("Ola Aina", "DF", "Nottingham Forest", 1, 0)));

        assertThat(plan.added()).extracting(Player::id).containsExactly("norway-ola-aina-2");
    }

    @Test
    void normalizesNordicAndAccentedLetters() {
        assertThat(SquadMerge.normalize("Martin Ødegaard")).isEqualTo("martin-odegaard");
        assertThat(SquadMerge.normalize("Kristoffer Ajer-Ærø")).isEqualTo("kristoffer-ajer-aero");
        assertThat(SquadMerge.normalize("Álvaro Morata")).isEqualTo("alvaro-morata");
    }
}
