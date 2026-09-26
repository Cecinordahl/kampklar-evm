package no.kampklar.evm.model;

/** Embedded in {@link Team}, e.g. ("EURO", 2024, "Kvartfinale"). */
public record TournamentHistoryEntry(String tournament, int year, String result) {
}
