package no.kampklar.evm.dto;

import no.kampklar.evm.model.Standing;

import java.util.List;

/** Returns the recomputed table so the admin view can show the effect of the save immediately. */
public record MatchSavedResponse(String matchId, String groupId, List<Standing> standings) {
}
