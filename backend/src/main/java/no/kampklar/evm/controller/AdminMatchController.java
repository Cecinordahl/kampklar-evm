package no.kampklar.evm.controller;

import jakarta.validation.Valid;
import no.kampklar.evm.dto.MatchRequest;
import no.kampklar.evm.dto.MatchSavedResponse;
import no.kampklar.evm.model.Standing;
import no.kampklar.evm.service.MatchService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PUT rather than POST: the admin picks the match id (e.g. "unl-2026-a1-nor-esp"), so saving
 * the same match twice - or correcting it later - is an idempotent upsert, not a duplicate.
 */
@RestController
@RequestMapping("/admin/matches")
public class AdminMatchController {

    private final MatchService matchService;

    public AdminMatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PutMapping("/{matchId}")
    public MatchSavedResponse save(@PathVariable String matchId, @Valid @RequestBody MatchRequest request) {
        List<Standing> standings = matchService.save(request.toMatch(matchId));
        return new MatchSavedResponse(matchId, request.groupId(), standings);
    }
}
