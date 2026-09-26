package no.kampklar.evm.model;

/** Embedded in {@link Team} - a team has exactly one head coach at a time. */
public record Coach(String name, String nationality) {
}
