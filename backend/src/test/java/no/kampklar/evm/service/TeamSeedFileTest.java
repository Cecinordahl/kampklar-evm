package no.kampklar.evm.service;

import no.kampklar.evm.model.Player;
import no.kampklar.evm.model.Team;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the hand-maintained team seed file, and how it maps onto our model, before it reaches Firestore. */
class TeamSeedFileTest {

    private final TeamSeeder.SeedFile seed = load();

    @Test
    void everyTeamMapsToAKnownTeamIdAndHasAParseableRefreshDate() {
        assertThat(seed.teams()).hasSize(3);
        seed.teams().values().forEach(t -> {
            assertThat(TeamSeeder.teamId(t)).isIn("norway", "spain", "france");
            assertThat(LocalDate.parse(t.lastRefreshedAt())).isNotNull();
            assertThat(LocalDate.parse(t.statsAsOf())).isNotNull();
        });
    }

    @Test
    void everyPlayerHasAValidPositionStatusAndBirthInfoAndAUniqueKey() {
        for (TeamSeeder.SeedTeam team : seed.teams().values()) {
            Set<String> keys = new HashSet<>();
            for (TeamSeeder.SeedPlayer p : team.players()) {
                assertThat(p.position()).as(p.name()).isIn("GK", "DF", "MF", "FW");
                assertThat(p.squadStatus()).as(p.name()).isIn("confirmed", "considered");
                if (p.birthDate() != null) {
                    assertThat(LocalDate.parse(p.birthDate())).as(p.name()).isNotNull();
                }
                // Two players normalizing to the same key would be merged into one document.
                assertThat(keys.add(SquadMerge.normalize(p.name()))).as(p.name()).isTrue();
            }
        }
    }

    @Test
    void refreshDateIsMidnightInOsloAndCoachNationalityIsLeftUnknown() {
        TeamSeeder.SeedTeam norway = seed.teams().get("nor");

        Team team = TeamSeeder.toTeam("norway", norway);

        assertThat(team.refreshedAt()).hasToString("2026-09-28T22:00:00Z");
        assertThat(team.coach().nationality()).isNull();
        assertThat(team.coach().record()).isNotNull();
        assertThat(team.tournamentHistory()).allSatisfy(h -> assertThat(h.detail()).isNotBlank());
    }

    @Test
    void unknownValuesStayNullAndConsideredPlayersAreNotInTheSquad() {
        TeamSeeder.SeedPlayer butez = seed.teams().get("fra").players().stream()
                .filter(p -> p.name().equals("Jean Butez")).findFirst().orElseThrow();

        Player player = TeamSeeder.toPlayer("france-jean-butez", "france", butez);

        assertThat(player.caps()).isNull();
        assertThat(player.goals()).isNull();
        assertThat(player.birthDate()).isNull();
        assertThat(player.birthYear()).isEqualTo(1995);
        assertThat(player.birthYearUnverified()).isTrue();
        assertThat(player.inSquad()).isEqualTo("confirmed".equals(butez.squadStatus()));
    }

    private static TeamSeeder.SeedFile load() {
        try (InputStream in = new ClassPathResource(TeamSeeder.SEED_FILE).getInputStream()) {
            return JsonMapper.builder().build().readValue(in, TeamSeeder.SeedFile.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
