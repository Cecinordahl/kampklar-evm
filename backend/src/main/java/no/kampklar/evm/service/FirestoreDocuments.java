package no.kampklar.evm.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import no.kampklar.evm.model.Coach;
import no.kampklar.evm.model.Group;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.MatchStatus;
import no.kampklar.evm.model.Player;
import no.kampklar.evm.model.Standing;
import no.kampklar.evm.model.TournamentHistoryEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Explicit mapping between domain records and Firestore documents. Kept hand-written rather
 * than relying on the SDK's reflective mapper so the stored field names are visible in one
 * place - the frontend reads these documents directly and depends on them.
 */
final class FirestoreDocuments {

    static final String GROUPS = "groups";
    static final String TEAMS = "teams";
    static final String MATCHES = "matches";
    static final String PLAYERS = "players";

    private FirestoreDocuments() {
    }

    static Group toGroup(DocumentSnapshot doc) {
        List<Map<String, Object>> standings = listOf(doc.get("standings"));
        return new Group(
                doc.getId(),
                doc.getString("competitionId"),
                doc.getString("divisionId"),
                doc.getString("name"),
                listOf(doc.get("teamIds")),
                standings.stream().map(FirestoreDocuments::toStanding).toList());
    }

    static Player toPlayer(DocumentSnapshot doc) {
        return new Player(
                doc.getId(),
                doc.getString("teamId"),
                doc.getString("name"),
                doc.getString("position"),
                doc.getString("club"),
                intOrZero(doc.getLong("caps")),
                intOrZero(doc.getLong("goals")),
                !Boolean.FALSE.equals(doc.getBoolean("inSquad")));
    }

    static Map<String, Object> fromPlayer(Player player) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("teamId", player.teamId());
        fields.put("name", player.name());
        fields.put("position", player.position());
        fields.put("club", player.club());
        fields.put("caps", player.caps());
        fields.put("goals", player.goals());
        fields.put("inSquad", player.inSquad());
        return fields;
    }

    static Map<String, Object> fromCoach(Coach coach) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", coach.name());
        fields.put("nationality", coach.nationality());
        return fields;
    }

    static List<Map<String, Object>> fromTournamentHistory(List<TournamentHistoryEntry> entries) {
        return entries.stream()
                .map(e -> Map.<String, Object>of("tournament", e.tournament(), "year", e.year(), "result", e.result()))
                .toList();
    }

    static Match toMatch(DocumentSnapshot doc) {
        Timestamp kickoff = doc.getTimestamp("kickoff");
        return new Match(
                doc.getId(),
                doc.getString("competitionId"),
                doc.getString("groupId"),
                doc.getString("homeTeamId"),
                doc.getString("awayTeamId"),
                kickoff == null ? null : kickoff.toSqlTimestamp().toInstant(),
                MatchStatus.valueOf(doc.getString("status")),
                intOrNull(doc.getLong("homeGoals")),
                intOrNull(doc.getLong("awayGoals")),
                listOf(doc.get("homeLineup")),
                listOf(doc.get("awayLineup")),
                listOf(doc.get("homeScorerIds")),
                listOf(doc.get("awayScorerIds")));
    }

    static Map<String, Object> fromMatch(Match match) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("competitionId", match.competitionId());
        fields.put("groupId", match.groupId());
        fields.put("homeTeamId", match.homeTeamId());
        fields.put("awayTeamId", match.awayTeamId());
        fields.put("kickoff", Timestamp.ofTimeSecondsAndNanos(
                match.kickoff().getEpochSecond(), match.kickoff().getNano()));
        fields.put("status", match.status().name());
        fields.put("homeGoals", match.homeGoals());
        fields.put("awayGoals", match.awayGoals());
        fields.put("homeLineup", match.homeLineup());
        fields.put("awayLineup", match.awayLineup());
        fields.put("homeScorerIds", match.homeScorerIds());
        fields.put("awayScorerIds", match.awayScorerIds());
        return fields;
    }

    static List<Map<String, Object>> fromStandings(List<Standing> standings) {
        return standings.stream().map(FirestoreDocuments::fromStanding).toList();
    }

    private static Map<String, Object> fromStanding(Standing s) {
        return Map.of(
                "teamId", s.teamId(),
                "rank", s.rank(),
                "played", s.played(),
                "won", s.won(),
                "drawn", s.drawn(),
                "lost", s.lost(),
                "goalsFor", s.goalsFor(),
                "goalsAgainst", s.goalsAgainst(),
                "goalDifference", s.goalDifference(),
                "points", s.points());
    }

    private static Standing toStanding(Map<String, Object> m) {
        return new Standing(
                (String) m.get("teamId"),
                number(m, "rank"),
                number(m, "played"),
                number(m, "won"),
                number(m, "drawn"),
                number(m, "lost"),
                number(m, "goalsFor"),
                number(m, "goalsAgainst"),
                number(m, "goalDifference"),
                number(m, "points"));
    }

    // Firestore stores all integers as 64-bit, so they come back as Long.
    private static int number(Map<String, Object> m, String key) {
        return ((Number) m.get(key)).intValue();
    }

    private static int intOrZero(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private static Integer intOrNull(Long value) {
        return value == null ? null : value.intValue();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> listOf(Object value) {
        return value == null ? List.of() : (List<T>) value;
    }
}
