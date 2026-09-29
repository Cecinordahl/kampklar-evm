package no.kampklar.evm.service;

import no.kampklar.evm.dto.ResultSuggestion;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.MatchStatus;
import no.kampklar.evm.model.Player;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultSuggestionsTest {

    private static final Match MATCH = new Match("m1", "unl", "a1", "nor", "esp",
            Instant.parse("2026-09-24T18:45:00Z"), MatchStatus.SCHEDULED, null, null,
            List.of(), List.of(), List.of(), List.of());

    private static final List<Player> NOR = List.of(
            player("nor-odegaard", "nor", "Martin Ødegaard"),
            player("nor-haaland", "nor", "Erling Haaland"));
    private static final List<Player> ESP = List.of(player("esp-pedri", "esp", "Pedri"));

    @Test
    void mapsNamesToSquadIdsIgnoringDiacritics() {
        var result = new ResultResearch.Result("m1", true, 2, 1,
                List.of("Martin Odegaard", "Erling Haaland"), List.of("Pedri"),
                List.of("Erling Haaland", "Erling Haaland"), List.of("Pedri"));

        ResultSuggestion suggestion = ResultSuggestions.from(result, MATCH, "Norge", NOR, "Spania", ESP);

        assertThat(suggestion.homeLineup()).containsExactly("nor-odegaard", "nor-haaland");
        assertThat(suggestion.homeScorerIds()).containsExactly("nor-haaland", "nor-haaland");
        assertThat(suggestion.awayScorerIds()).containsExactly("esp-pedri");
        assertThat(suggestion.warnings()).isEmpty();
    }

    @Test
    void dropsAndWarnsAboutPlayersOutsideTheStoredSquad() {
        var result = new ResultResearch.Result("m1", true, 1, 0,
                List.of("Erling Haaland", "Oscar Bobb"), List.of("Pedri"),
                List.of("Oscar Bobb"), List.of());

        ResultSuggestion suggestion = ResultSuggestions.from(result, MATCH, "Norge", NOR, "Spania", ESP);

        assertThat(suggestion.homeLineup()).containsExactly("nor-haaland");
        assertThat(suggestion.homeScorerIds()).isEmpty(); // never a scorer outside the lineup
        assertThat(suggestion.warnings()).hasSize(2);
    }

    @Test
    void warnsInsteadOfGuessingWhenATeamHasNoSquad() {
        var result = new ResultResearch.Result("m1", true, 0, 0,
                List.of("Erling Haaland"), List.of("Pedri"), List.of(), List.of());

        ResultSuggestion suggestion = ResultSuggestions.from(result, MATCH, "Norge", List.of(), "Spania", ESP);

        assertThat(suggestion.homeLineup()).isEmpty();
        assertThat(suggestion.warnings()).singleElement().asString().contains("Oppdater lagdata");
    }

    private static Player player(String id, String teamId, String name) {
        return new Player(id, teamId, name, "FW", "Club", 10, 2, true);
    }
}
