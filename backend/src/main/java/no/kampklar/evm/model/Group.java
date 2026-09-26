package no.kampklar.evm.model;

import java.util.List;

/**
 * Firestore: {@code groups/{id}}. The computed table is stored on the group document itself
 * rather than derived client-side, so a public group page is a single document read.
 */
public record Group(
        String id,
        String competitionId,
        String divisionId,
        String name,
        List<String> teamIds,
        List<Standing> standings
) {
}
