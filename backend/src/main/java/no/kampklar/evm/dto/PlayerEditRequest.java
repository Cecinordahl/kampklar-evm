package no.kampklar.evm.dto;

/**
 * Body of PUT /admin/players/{playerId}: every manually editable field, sent in full (the form
 * always has all current values), so null unambiguously means "unknown" rather than "unchanged".
 *
 * <p>{@code caps}/{@code goals} are career totals including matches entered here. Normally match
 * entry owns them; setting them here is a baseline correction (e.g. a source that was wrong).
 */
public record PlayerEditRequest(
        String club,
        String position,
        String birthDate,
        Integer birthYear,
        String squadStatus,
        boolean captain,
        String note,
        Integer caps,
        Integer goals
) {
}
