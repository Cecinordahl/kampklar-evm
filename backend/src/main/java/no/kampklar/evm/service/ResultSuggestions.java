package no.kampklar.evm.service;

import no.kampklar.evm.dto.ResultSuggestion;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a researched result (player names) into a suggestion (player ids) that passes
 * {@link MatchValidator}. Names are matched against the team's current squad by normalized
 * name, like {@link SquadMerge}; anything unmatched is dropped and reported as a warning.
 */
final class ResultSuggestions {

    private ResultSuggestions() {
    }

    static ResultSuggestion from(ResultResearch.Result result, Match match,
                                 String homeName, List<Player> homeSquad,
                                 String awayName, List<Player> awaySquad) {
        List<String> warnings = new ArrayList<>();
        Side home = mapSide(result.homeLineup(), result.homeScorers(), result.homeGoals(), homeName, homeSquad, warnings);
        Side away = mapSide(result.awayLineup(), result.awayScorers(), result.awayGoals(), awayName, awaySquad, warnings);
        return new ResultSuggestion(match.id(), result.homeGoals(), result.awayGoals(),
                home.lineup(), away.lineup(), home.scorerIds(), away.scorerIds(), warnings);
    }

    private record Side(List<String> lineup, List<String> scorerIds) {
    }

    private static Side mapSide(List<String> lineupNames, List<String> scorerNames, int goals,
                                String teamName, List<Player> squad, List<String> warnings) {
        if (squad.isEmpty()) {
            warnings.add("Ingen tropp lagret for " + teamName + " - kjør Oppdater lagdata først.");
            return new Side(List.of(), List.of());
        }
        Map<String, String> idByName = new HashMap<>();
        for (Player player : squad) {
            idByName.put(SquadMerge.normalize(player.name()), player.id());
        }

        Set<String> lineup = new LinkedHashSet<>();
        for (String name : orEmpty(lineupNames)) {
            String id = idByName.get(SquadMerge.normalize(name));
            if (id == null) {
                warnings.add(name + " (" + teamName + ") spilte, men er ikke i den lagrede troppen.");
            } else {
                lineup.add(id);
            }
        }

        List<String> scorerIds = new ArrayList<>();
        for (String name : orEmpty(scorerNames)) {
            String id = idByName.get(SquadMerge.normalize(name));
            if (id == null || !lineup.contains(id)) {
                warnings.add("Målscorer " + name + " (" + teamName + ") ble ikke registrert.");
            } else if (scorerIds.size() < goals) {
                scorerIds.add(id);
            }
        }
        return new Side(List.copyOf(lineup), scorerIds);
    }

    private static List<String> orEmpty(List<String> list) {
        return list == null ? List.of() : list;
    }
}
