package no.kampklar.evm.service;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * What Claude returns for "Hent resultater med AI". The record shapes double as the JSON schema
 * the API constrains its answer to, so the descriptions below are part of the prompt.
 */
public record ResultResearch(
        @JsonPropertyDescription("One entry per match id you were given")
        List<Result> results
) {

    public record Result(
            @JsonPropertyDescription("The match id exactly as given")
            String matchId,
            @JsonPropertyDescription("True only if the match has been played to full time and the sources agree on the score")
            boolean played,
            @JsonPropertyDescription("Home team goals at full time; 0 if not played")
            int homeGoals,
            @JsonPropertyDescription("Away team goals at full time; 0 if not played")
            int awayGoals,
            @JsonPropertyDescription("Every home player who took part: the starting eleven plus substitutes who came on")
            List<String> homeLineup,
            @JsonPropertyDescription("Every away player who took part: the starting eleven plus substitutes who came on")
            List<String> awayLineup,
            @JsonPropertyDescription("Home goalscorers, one entry per goal (a brace appears twice); own goals are left out")
            List<String> homeScorers,
            @JsonPropertyDescription("Away goalscorers, one entry per goal (a brace appears twice); own goals are left out")
            List<String> awayScorers
    ) {
    }
}
