package no.kampklar.evm.service;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import no.kampklar.evm.model.Coach;
import no.kampklar.evm.model.TournamentHistoryEntry;

import java.util.List;

/**
 * What Claude returns for "Oppdater lagdata". The record shapes double as the JSON schema the
 * API constrains its answer to, so the descriptions below are part of the prompt.
 */
public record TeamResearch(
        @JsonPropertyDescription("The current head coach")
        Coach coach,
        @JsonPropertyDescription("Every player in the most recent official squad announcement")
        List<Player> squad,
        @JsonPropertyDescription("One entry per EURO and World Cup final tournament since 2000, including ones the team did not qualify for")
        List<TournamentHistoryEntry> tournamentHistory
) {

    public record Player(
            @JsonPropertyDescription("Full name as commonly written, with original diacritics")
            String name,
            @JsonPropertyDescription("One of GK, DF, MF, FW")
            String position,
            @JsonPropertyDescription("Current club")
            String club,
            @JsonPropertyDescription("Career senior national team appearances")
            int caps,
            @JsonPropertyDescription("Career senior national team goals")
            int goals
    ) {
    }
}
