package no.kampklar.evm.service;

import no.kampklar.evm.model.Group;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Consistency rules for a match about to be saved. Pure functions over already-loaded
 * documents, so they run inside the Firestore transaction without extra reads.
 */
final class MatchValidator {

    private MatchValidator() {
    }

    static void validate(Match match, Group group, Match previous) {
        if (match.homeTeamId().equals(match.awayTeamId())) {
            throw new InvalidMatchException("A team cannot play itself");
        }
        if (!group.competitionId().equals(match.competitionId())) {
            throw new InvalidMatchException("Group " + group.id() + " is not in competition " + match.competitionId());
        }
        if (!group.teamIds().contains(match.homeTeamId()) || !group.teamIds().contains(match.awayTeamId())) {
            throw new InvalidMatchException("Both teams must belong to group " + group.id());
        }
        if (previous != null && !previous.groupId().equals(match.groupId())) {
            // Moving a match would leave the old group's stored table stale.
            throw new InvalidMatchException("A match cannot be moved to another group");
        }
        if (match.isFinished()) {
            validateFinished(match);
        } else {
            validateScheduled(match);
        }
    }

    /** Every player in a lineup must exist and belong to the team they are listed for. */
    static void validatePlayers(Match match, Map<String, Player> playersById) {
        checkPlayersBelongTo(match.homeLineup(), match.homeTeamId(), playersById);
        checkPlayersBelongTo(match.awayLineup(), match.awayTeamId(), playersById);
    }

    private static void validateFinished(Match match) {
        if (match.homeGoals() == null || match.awayGoals() == null) {
            throw new InvalidMatchException("A finished match needs both scores");
        }
        checkNoDuplicates(match.homeLineup(), "home lineup");
        checkNoDuplicates(match.awayLineup(), "away lineup");
        if (match.homeLineup().stream().anyMatch(match.awayLineup()::contains)) {
            throw new InvalidMatchException("A player cannot be in both lineups");
        }
        checkScorers(match.homeScorerIds(), match.homeLineup(), match.homeGoals(), "home");
        checkScorers(match.awayScorerIds(), match.awayLineup(), match.awayGoals(), "away");
    }

    private static void validateScheduled(Match match) {
        boolean hasResultData = match.homeGoals() != null || match.awayGoals() != null
                || !match.homeLineup().isEmpty() || !match.awayLineup().isEmpty()
                || !match.homeScorerIds().isEmpty() || !match.awayScorerIds().isEmpty();
        if (hasResultData) {
            throw new InvalidMatchException("A scheduled match cannot have scores, lineups or scorers");
        }
    }

    private static void checkScorers(List<String> scorerIds, List<String> lineup, int goals, String side) {
        if (scorerIds.size() > goals) {
            throw new InvalidMatchException("More " + side + " scorers than " + side + " goals");
        }
        for (String scorerId : scorerIds) {
            if (!lineup.contains(scorerId)) {
                throw new InvalidMatchException("Scorer " + scorerId + " is not in the " + side + " lineup");
            }
        }
    }

    private static void checkNoDuplicates(List<String> playerIds, String label) {
        Set<String> seen = new HashSet<>();
        for (String playerId : playerIds) {
            if (!seen.add(playerId)) {
                throw new InvalidMatchException("Player " + playerId + " appears twice in the " + label);
            }
        }
    }

    private static void checkPlayersBelongTo(List<String> lineup, String teamId, Map<String, Player> playersById) {
        for (String playerId : lineup) {
            Player player = playersById.get(playerId);
            if (player == null) {
                throw new InvalidMatchException("Unknown player " + playerId);
            }
            if (!player.teamId().equals(teamId)) {
                throw new InvalidMatchException("Player " + playerId + " does not play for " + teamId);
            }
        }
    }
}
