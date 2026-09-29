package no.kampklar.evm.model;

/** Embedded in {@link Team}, e.g. ("EM", 2024, "Kvartfinale", "Tapte 1–2 mot ..."). Detail may be null. */
public record TournamentHistoryEntry(String tournament, int year, String result, String detail) {
}
