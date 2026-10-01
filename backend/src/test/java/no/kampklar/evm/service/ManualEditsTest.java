package no.kampklar.evm.service;

import no.kampklar.evm.dto.PlayerEditRequest;
import no.kampklar.evm.dto.TeamDetailsRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualEditsTest {

    private static PlayerEditRequest player(String position, String birthDate, Integer birthYear, String squadStatus,
                                            Integer caps, Integer goals) {
        return new PlayerEditRequest("  Arsenal ", position, birthDate, birthYear, squadStatus, true, " ", caps, goals);
    }

    @Test
    void unknownValuesStayNullAndBlankTextBecomesNull() {
        Map<String, Object> fields = ManualEdits.playerFields(player("MF", "", null, "confirmed", null, null));

        assertThat(fields).containsEntry("club", "Arsenal").containsEntry("note", null)
                .containsEntry("caps", null).containsEntry("goals", null)
                .containsEntry("birthDate", null).containsEntry("birthYear", null);
    }

    @Test
    void birthYearFollowsTheBirthDateAndIsThenVerified() {
        Map<String, Object> fields = ManualEdits.playerFields(player("GK", "2005-03-14", 2004, "confirmed", 3, 0));

        assertThat(fields).containsEntry("birthYear", 2005).containsEntry("birthYearUnverified", false);
    }

    @Test
    void inSquadFollowsTheSquadStatus() {
        assertThat(ManualEdits.playerFields(player("FW", null, null, "confirmed", 1, 0))).containsEntry("inSquad", true);
        assertThat(ManualEdits.playerFields(player("FW", null, null, "considered", 1, 0))).containsEntry("inSquad", false);
        assertThat(ManualEdits.playerFields(player("FW", null, null, "", 1, 0)))
                .containsEntry("inSquad", false).containsEntry("squadStatus", null);
    }

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> ManualEdits.playerFields(player("ST", null, null, "confirmed", 1, 0)))
                .isInstanceOf(InvalidEditException.class).hasMessageContaining("Posisjon");
        assertThatThrownBy(() -> ManualEdits.playerFields(player("FW", "14.03.2005", null, "confirmed", 1, 0)))
                .isInstanceOf(InvalidEditException.class).hasMessageContaining("Fødselsdato");
        assertThatThrownBy(() -> ManualEdits.playerFields(player("FW", null, null, "injured", 1, 0)))
                .isInstanceOf(InvalidEditException.class).hasMessageContaining("Troppstatus");
        assertThatThrownBy(() -> ManualEdits.playerFields(player("FW", null, null, "confirmed", -1, 0)))
                .isInstanceOf(InvalidEditException.class).hasMessageContaining("negative");
    }

    @Test
    void teamDetailsUseDottedCoachPathsSoTheRecordIsKept() {
        Map<String, Object> fields = ManualEdits.teamDetailFields(new TeamDetailsRequest(
                "Luis de la Fuente", "Spania", "1961-06-21", "2022-12", " ", Arrays.asList("Note 1", " ", null)));

        assertThat(fields).doesNotContainKey("coach").doesNotContainKey("coach.record")
                .containsEntry("coach.appointedDate", "2022-12").containsEntry("coach.bio", null)
                .containsEntry("dataNotes", List.of("Note 1"));
        assertThatThrownBy(() -> ManualEdits.teamDetailFields(new TeamDetailsRequest(" ", null, null, null, null, null)))
                .isInstanceOf(InvalidEditException.class);
    }
}
