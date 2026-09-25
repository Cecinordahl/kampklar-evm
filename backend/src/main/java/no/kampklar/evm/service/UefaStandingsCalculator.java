package no.kampklar.evm.service;

import no.kampklar.evm.model.MatchResult;
import no.kampklar.evm.model.Standing;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes a group table using UEFA's head-to-head-first tiebreak order - NOT FIFA's order,
 * which compares overall goal difference before head-to-head results. The two orders can
 * disagree (see the unit tests), so this is kept as one explicit, isolated function rather
 * than inlined into whichever service happens to need a sorted table.
 *
 * <p>Per UEFA regulations, when two or more teams are equal on points, ranking is decided by,
 * in order: (a) points from matches among the tied teams only; (b) goal difference in those
 * same matches; (c) goals scored in those same matches; (d) if a subset of the tied teams is
 * still equal after (a)-(c), criteria (a)-(c) are reapplied using only matches between that
 * subset - implemented below as recursion. Only once head-to-head comparison fails to separate
 * anyone at all does the group fall through to (e) overall goal difference, (f) overall goals
 * scored, (g) overall wins, in the whole group's matches. Disciplinary points and the UEFA
 * coefficient ranking (the regulation's final criteria) are not modeled in this MVP; a
 * deterministic alphabetical-by-teamId order is used as the last resort instead.
 */
@Service
public class UefaStandingsCalculator {

    public List<Standing> compute(List<String> teamIds, List<MatchResult> matches) {
        Map<String, Accumulator> overallStats = aggregate(teamIds, matches);

        Comparator<String> byPoints = Comparator.comparingInt((String id) -> overallStats.get(id).points()).reversed();
        List<List<String>> pointsGroups = groupByComparator(teamIds, byPoints);

        List<Standing> result = new ArrayList<>();
        int rank = 1;
        for (List<String> pointsGroup : pointsGroups) {
            for (String teamId : resolveTiedGroup(pointsGroup, matches, overallStats)) {
                result.add(toStanding(teamId, overallStats.get(teamId), rank++));
            }
        }
        return result;
    }

    private List<String> resolveTiedGroup(List<String> tiedTeamIds, List<MatchResult> allMatches,
                                           Map<String, Accumulator> overallStats) {
        if (tiedTeamIds.size() <= 1) {
            return tiedTeamIds;
        }

        List<MatchResult> headToHeadMatches = onlyBetween(allMatches, tiedTeamIds);
        Map<String, Accumulator> h2hStats = aggregate(tiedTeamIds, headToHeadMatches);

        Comparator<String> byHeadToHead = Comparator
                .comparingInt((String id) -> h2hStats.get(id).points()).reversed()
                .thenComparing(Comparator.comparingInt((String id) -> h2hStats.get(id).goalDifference()).reversed())
                .thenComparing(Comparator.comparingInt((String id) -> h2hStats.get(id).goalsFor).reversed());

        List<List<String>> h2hGroups = groupByComparator(tiedTeamIds, byHeadToHead);

        if (h2hGroups.size() == 1) {
            // Head-to-head comparison did not separate anyone in this subgroup - fall through
            // to overall-group criteria (e)-(g) instead of recursing forever.
            return resolveByOverallCriteria(tiedTeamIds, overallStats);
        }

        List<String> resolved = new ArrayList<>();
        for (List<String> subgroup : h2hGroups) {
            resolved.addAll(resolveTiedGroup(subgroup, allMatches, overallStats));
        }
        return resolved;
    }

    private List<String> resolveByOverallCriteria(List<String> tiedTeamIds, Map<String, Accumulator> overallStats) {
        List<String> sorted = new ArrayList<>(tiedTeamIds);
        sorted.sort(Comparator
                .comparingInt((String id) -> overallStats.get(id).goalDifference()).reversed()
                .thenComparing(Comparator.comparingInt((String id) -> overallStats.get(id).goalsFor).reversed())
                .thenComparing(Comparator.comparingInt((String id) -> overallStats.get(id).won).reversed())
                .thenComparing(Comparator.naturalOrder()));
        return sorted;
    }

    /** Sorts by the comparator, then groups consecutive teams the comparator ranks as fully tied. */
    private List<List<String>> groupByComparator(List<String> teamIds, Comparator<String> comparator) {
        List<String> sorted = new ArrayList<>(teamIds);
        sorted.sort(comparator);

        List<List<String>> groups = new ArrayList<>();
        for (String teamId : sorted) {
            if (groups.isEmpty() || comparator.compare(groups.get(groups.size() - 1).get(0), teamId) != 0) {
                List<String> group = new ArrayList<>();
                group.add(teamId);
                groups.add(group);
            } else {
                groups.get(groups.size() - 1).add(teamId);
            }
        }
        return groups;
    }

    private List<MatchResult> onlyBetween(List<MatchResult> matches, List<String> teamIds) {
        return matches.stream()
                .filter(m -> teamIds.contains(m.homeTeamId()) && teamIds.contains(m.awayTeamId()))
                .toList();
    }

    private Map<String, Accumulator> aggregate(List<String> teamIds, List<MatchResult> matches) {
        Map<String, Accumulator> stats = new LinkedHashMap<>();
        for (String teamId : teamIds) {
            stats.put(teamId, new Accumulator());
        }
        for (MatchResult match : matches) {
            Accumulator home = stats.get(match.homeTeamId());
            Accumulator away = stats.get(match.awayTeamId());
            if (home == null || away == null) {
                continue;
            }
            home.record(match.homeGoals(), match.awayGoals());
            away.record(match.awayGoals(), match.homeGoals());
        }
        return stats;
    }

    private Standing toStanding(String teamId, Accumulator acc, int rank) {
        return new Standing(teamId, rank, acc.played, acc.won, acc.drawn, acc.lost,
                acc.goalsFor, acc.goalsAgainst, acc.goalDifference(), acc.points());
    }

    private static final class Accumulator {
        int played;
        int won;
        int drawn;
        int lost;
        int goalsFor;
        int goalsAgainst;

        void record(int scored, int conceded) {
            played++;
            goalsFor += scored;
            goalsAgainst += conceded;
            if (scored > conceded) {
                won++;
            } else if (scored == conceded) {
                drawn++;
            } else {
                lost++;
            }
        }

        int goalDifference() {
            return goalsFor - goalsAgainst;
        }

        int points() {
            return won * 3 + drawn;
        }
    }
}
