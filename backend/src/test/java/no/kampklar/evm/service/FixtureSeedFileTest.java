package no.kampklar.evm.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the hand-maintained seed file against typos before it reaches Firestore. */
class FixtureSeedFileTest {

    private final FixtureSeeder.SeedFile seed = load();

    @Test
    void everyGroupPlaysAFullDoubleRoundRobin() {
        for (FixtureSeeder.SeedFile.Group group : seed.groups()) {
            Set<String> pairings = seed.matches().stream()
                    .filter(m -> m.groupId().equals(group.id()))
                    .map(m -> m.homeTeamId() + ">" + m.awayTeamId())
                    .collect(Collectors.toSet());

            // 4 teams, each hosting each other team once = 12 distinct home/away pairings.
            assertThat(pairings).as(group.name()).hasSize(12);
        }
    }

    @Test
    void everyFixtureIsBetweenTwoTeamsOfItsOwnGroup() {
        Map<String, FixtureSeeder.SeedFile.Group> groups = seed.groups().stream()
                .collect(Collectors.toMap(FixtureSeeder.SeedFile.Group::id, Function.identity()));

        for (FixtureSeeder.SeedFile.Fixture fixture : seed.matches()) {
            FixtureSeeder.SeedFile.Group group = groups.get(fixture.groupId());
            assertThat(group).as(fixture.id()).isNotNull();
            assertThat(group.teamIds()).as(fixture.id()).contains(fixture.homeTeamId(), fixture.awayTeamId());
        }
    }

    @Test
    void everyReferencedTeamHasADisplayNameAndEveryKickoffParses() {
        Set<String> teamIds = seed.teams().stream().map(FixtureSeeder.SeedFile.Team::id).collect(Collectors.toSet());

        seed.groups().forEach(g -> assertThat(teamIds).containsAll(g.teamIds()));
        seed.matches().forEach(m -> assertThat(LocalDateTime.parse(m.localKickoff())).isNotNull());
    }

    private static FixtureSeeder.SeedFile load() {
        try (InputStream in = new ClassPathResource("seed/unl-2026-27.json").getInputStream()) {
            return JsonMapper.builder().build().readValue(in, FixtureSeeder.SeedFile.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
