package no.kampklar.evm.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import no.kampklar.evm.model.Coach;
import no.kampklar.evm.model.CoachRecord;
import no.kampklar.evm.model.Group;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.MatchStatus;
import no.kampklar.evm.model.Player;
import no.kampklar.evm.model.Standing;
import no.kampklar.evm.model.Team;
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
                doc.getString("birthDate"),
                intOrNull(doc.getLong("birthYear")),
                Boolean.TRUE.equals(doc.getBoolean("birthYearUnverified")),
                intOrNull(doc.getLong("caps")),
                intOrNull(doc.getLong("goals")),
                !Boolean.FALSE.equals(doc.getBoolean("inSquad")),
                doc.getString("squadStatus"),
                Boolean.TRUE.equals(doc.getBoolean("captain")),
                doc.getString("note"));
    }

    /** Every field; nulls are stored as nulls so "unknown" survives the round trip. */
    static Map<String, Object> fromPlayer(Player player) {
        Map<String, Object> fields = fromPlayerExceptStats(player);
        fields.put("caps", player.caps());
        fields.put("goals", player.goals());
        return fields;
    }

    /** Everything but caps/goals, which match entry owns once a player exists. */
    static Map<String, Object> fromPlayerExceptStats(Player player) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("teamId", player.teamId());
        fields.put("name", player.name());
        fields.put("position", player.position());
        fields.put("club", player.club());
        fields.put("birthDate", player.birthDate());
        fields.put("birthYear", player.birthYear());
        fields.put("birthYearUnverified", player.birthYearUnverified());
        fields.put("inSquad", player.inSquad());
        fields.put("squadStatus", player.squadStatus());
        fields.put("captain", player.captain());
        fields.put("note", player.note());
        return fields;
    }

    static Map<String, Object> fromCoach(Coach coach) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", coach.name());
        fields.put("nationality", coach.nationality());
        fields.put("birthDate", coach.birthDate());
        fields.put("appointedDate", coach.appointedDate());
        fields.put("bio", coach.bio());
        CoachRecord r = coach.record();
        fields.put("record", r == null ? null : Map.of(
                "matches", r.matches(),
                "wins", r.wins(),
                "draws", r.draws(),
                "losses", r.losses(),
                "goalsFor", r.goalsFor(),
                "goalsAgainst", r.goalsAgainst(),
                "asOf", r.asOf()));
        return fields;
    }

    static List<Map<String, Object>> fromTournamentHistory(List<TournamentHistoryEntry> entries) {
        return entries.stream()
                .map(e -> {
                    Map<String, Object> fields = new HashMap<>();
                    fields.put("tournament", e.tournament());
                    fields.put("year", e.year());
                    fields.put("result", e.result());
                    fields.put("detail", e.detail());
                    return fields;
                })
                .toList();
    }

    /** Team-level fields only; the id is the document id. */
    static Map<String, Object> fromTeam(Team team) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", team.name());
        fields.put("fifaCode", team.fifaCode());
        fields.put("flagCountryCode", team.flagCountryCode());
        fields.put("currentCompetition", team.currentCompetition());
        fields.put("coach", team.coach() == null ? null : fromCoach(team.coach()));
        fields.put("tournamentHistory", fromTournamentHistory(team.tournamentHistory()));
        fields.put("statsAsOf", team.statsAsOf());
        fields.put("dataNotes", team.dataNotes());
        fields.put("refreshedAt", team.refreshedAt() == null ? null : Timestamp.ofTimeSecondsAndNanos(
                team.refreshedAt().getEpochSecond(), team.refreshedAt().getNano()));
        return fields;
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

    private static Integer intOrNull(Long value) {
        return value == null ? null : value.intValue();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> listOf(Object value) {
        return value == null ? List.of() : (List<T>) value;
    }
}
