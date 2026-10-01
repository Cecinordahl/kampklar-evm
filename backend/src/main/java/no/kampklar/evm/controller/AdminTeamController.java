package no.kampklar.evm.controller;

import no.kampklar.evm.dto.TeamDetailsRequest;
import no.kampklar.evm.dto.TeamRefreshResponse;
import no.kampklar.evm.model.Player;
import no.kampklar.evm.service.ManualEditService;
import no.kampklar.evm.service.TeamRefreshService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/teams")
public class AdminTeamController {

    private final TeamRefreshService teamRefreshService;
    private final ManualEditService manualEditService;

    public AdminTeamController(TeamRefreshService teamRefreshService, ManualEditService manualEditService) {
        this.teamRefreshService = teamRefreshService;
        this.manualEditService = manualEditService;
    }

    /** Manual correction of the coach's details and the data notes from admin mode. */
    @PutMapping("/{teamId}/details")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editDetails(@PathVariable String teamId, @RequestBody TeamDetailsRequest request) {
        manualEditService.editTeamDetails(teamId, request);
    }

    /** "Oppdater lagdata". Slow (typically 1-3 minutes) and costs a few tens of cents per call. */
    @PostMapping("/{teamId}/refresh")
    public TeamRefreshResponse refresh(@PathVariable String teamId) {
        TeamRefreshService.Result result = teamRefreshService.refresh(teamId);
        return new TeamRefreshResponse(
                teamId,
                result.coach(),
                result.tournamentsRecorded(),
                result.playersAdded().stream().map(Player::name).toList(),
                result.playersUpdated().size(),
                result.playersLeftSquad().stream().map(Player::name).toList());
    }
}
