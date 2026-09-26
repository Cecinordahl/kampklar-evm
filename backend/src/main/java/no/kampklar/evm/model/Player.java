package no.kampklar.evm.model;

/** Firestore: {@code players/{id}}. Caps and goals are career totals for the national team. */
public record Player(String id, String teamId, String name, String position, int caps, int goals) {
}
