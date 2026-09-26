package no.kampklar.evm.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteBatch;
import no.kampklar.evm.model.Match;
import no.kampklar.evm.model.MatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static no.kampklar.evm.service.FirestoreDocuments.GROUPS;
import static no.kampklar.evm.service.FirestoreDocuments.MATCHES;
import static no.kampklar.evm.service.FirestoreDocuments.TEAMS;

/**
 * Loads competitions, groups, teams and fixtures from {@code seed/unl-2026-27.json} into
 * Firestore. Run once per environment with the "seed" profile (see README); the app exits
 * when done.
 *
 * <p>Safe to re-run: it only ever adds missing documents or refreshes names. Existing matches
 * (and their results), group standings, and a team's coach/history/squad are never touched -
 * results always go through match entry so tables and caps stay consistent.
 */
@Component
@Profile("seed")
class FixtureSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FixtureSeeder.class);
    private static final String SEED_FILE = "seed/unl-2026-27.json";

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedFile(Competition competition, List<Division> divisions, String kickoffTimeZone,
                    List<Team> teams, List<Group> groups, List<Fixture> matches) {
        record Competition(String id, String name, String season) {
        }

        record Division(String id, String name) {
        }

        record Team(String id, String name) {
        }

        record Group(String id, String divisionId, String name, List<String> teamIds) {
        }

        record Fixture(String id, String groupId, String homeTeamId, String awayTeamId, String localKickoff) {
        }
    }

    private final Firestore firestore;
    private final JsonMapper jsonMapper;
    private final UefaStandingsCalculator standingsCalculator;

    FixtureSeeder(Firestore firestore, JsonMapper jsonMapper, UefaStandingsCalculator standingsCalculator) {
        this.firestore = firestore;
        this.jsonMapper = jsonMapper;
        this.standingsCalculator = standingsCalculator;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        SeedFile seed;
        try (InputStream in = new ClassPathResource(SEED_FILE).getInputStream()) {
            seed = jsonMapper.readValue(in, SeedFile.class);
        }
        String competitionId = seed.competition().id();
        ZoneId zone = ZoneId.of(seed.kickoffTimeZone());
        WriteBatch batch = firestore.batch();

        batch.set(firestore.collection("competitions").document(competitionId),
                Map.of("name", seed.competition().name(), "season", seed.competition().season()), SetOptions.merge());
        for (SeedFile.Division division : seed.divisions()) {
            batch.set(firestore.collection("divisions").document(division.id()),
                    Map.of("competitionId", competitionId, "name", division.name()), SetOptions.merge());
        }
        for (SeedFile.Team team : seed.teams()) {
            batch.set(firestore.collection(TEAMS).document(team.id()), Map.of("name", team.name()), SetOptions.merge());
        }

        int groupsCreated = 0;
        for (SeedFile.Group group : seed.groups()) {
            DocumentReference ref = firestore.collection(GROUPS).document(group.id());
            Map<String, Object> fields = Map.of(
                    "competitionId", competitionId,
                    "divisionId", group.divisionId(),
                    "name", group.name(),
                    "teamIds", group.teamIds());
            if (ref.get().get().exists()) {
                batch.set(ref, fields, SetOptions.merge()); // keeps the stored standings
            } else {
                // An all-zero table, so a group page has rows to show before the first result.
                batch.set(ref, Map.of(
                        "competitionId", competitionId,
                        "divisionId", group.divisionId(),
                        "name", group.name(),
                        "teamIds", group.teamIds(),
                        "standings", FirestoreDocuments.fromStandings(
                                standingsCalculator.compute(group.teamIds(), List.of()))));
                groupsCreated++;
            }
        }

        int matchesCreated = 0;
        for (SeedFile.Fixture fixture : seed.matches()) {
            DocumentReference ref = firestore.collection(MATCHES).document(fixture.id());
            DocumentSnapshot existing = ref.get().get();
            if (existing.exists()) {
                continue; // may already hold a result entered via match entry
            }
            Match match = new Match(fixture.id(), competitionId, fixture.groupId(), fixture.homeTeamId(),
                    fixture.awayTeamId(), LocalDateTime.parse(fixture.localKickoff()).atZone(zone).toInstant(),
                    MatchStatus.SCHEDULED, null, null, List.of(), List.of(), List.of(), List.of());
            batch.set(ref, FirestoreDocuments.fromMatch(match));
            matchesCreated++;
        }

        batch.commit().get();
        log.info("Seed complete: {} teams, {} groups ({} new), {} matches ({} new, {} already existed)",
                seed.teams().size(), seed.groups().size(), groupsCreated,
                seed.matches().size(), matchesCreated, seed.matches().size() - matchesCreated);
    }
}
