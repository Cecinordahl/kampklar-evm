package no.kampklar.evm.model;

/**
 * One team's row in a computed group table, already in final rank order (rank 1 = top of group).
 */
public record Standing(
        String teamId,
        int rank,
        int played,
        int won,
        int drawn,
        int lost,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        int points
) {
}
