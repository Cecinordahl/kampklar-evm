package no.kampklar.evm.model;

/** A coach's results in charge of the national team, as of {@code asOf} (ISO date). */
public record CoachRecord(int matches, int wins, int draws, int losses, int goalsFor, int goalsAgainst,
                          String asOf) {
}
