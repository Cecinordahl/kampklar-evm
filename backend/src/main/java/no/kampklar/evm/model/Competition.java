package no.kampklar.evm.model;

/** Firestore: {@code competitions/{id}}, e.g. "UEFA Nations League" / "2026/27". */
public record Competition(String id, String name, String season) {
}
