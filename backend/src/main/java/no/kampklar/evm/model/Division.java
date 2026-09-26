package no.kampklar.evm.model;

/** Firestore: {@code divisions/{id}}. Nations League "League" A-D, one level above groups. */
public record Division(String id, String competitionId, String name) {
}
