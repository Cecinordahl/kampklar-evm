import { useState } from "react";
import { refreshTeam, type TeamRefreshResult } from "../../lib/adminApi";
import { useAdminMode } from "../../lib/adminMode";
import { daysBetween, formatDate, formatDaysAgo, formatTime } from "../../lib/format";
import { LongTaskOverlay } from "./LongTaskOverlay";
import type { Team } from "../../types/firestore";

// A refresh costs real money; squads rarely change within a week outside squad announcements.
const RECENT_DAYS = 7;

/** "Oppdater lagdata med AI" on the team page: slow (1-3 min) and paid, so it says when it last ran. */
export function TeamRefreshPanel({ team }: { team: Team }) {
  const { ready } = useAdminMode();
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<TeamRefreshResult | null>(null);

  const { refreshedAt } = team;
  const recentlyRefreshed = refreshedAt !== null && daysBetween(refreshedAt, new Date()) < RECENT_DAYS;

  async function handleRefresh() {
    setRefreshing(true);
    setError(null);
    setResult(null);
    try {
      setResult(await refreshTeam(team.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ukjent feil.");
    } finally {
      setRefreshing(false);
    }
  }

  return (
    <div className="admin-panel">
      <div className="admin-toolbar">
        <button
          type="button"
          className="button button-secondary button-small"
          onClick={handleRefresh}
          disabled={!ready || refreshing}
          title={ready ? undefined : "Venter på serveren…"}
        >
          ✨ {recentlyRefreshed ? "Oppdater lagdata likevel" : "Oppdater lagdata med AI"}
        </button>
        <span className={recentlyRefreshed ? "notice small" : "muted small"}>
          {refreshedAt
            ? `Sist oppdatert ${formatDaysAgo(refreshedAt)} (${formatDate(refreshedAt)} kl. ${formatTime(refreshedAt)}).`
            : "Aldri oppdatert."}
          {recentlyRefreshed && " Hver oppdatering koster API-kreditt."}
        </span>
      </div>
      <p className="muted small">Henter landslagssjef, tropp og turneringshistorikk fra nettet og lagrer direkte.</p>

      {refreshing && <LongTaskOverlay title={`Oppdaterer lagdata for ${team.name}`} />}

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}

      {result && (
        <div className="admin-refresh-result">
          {result.coach && (
            <p>
              Landslagssjef: {result.coach.name}
              {result.coach.nationality && ` (${result.coach.nationality})`}
            </p>
          )}
          <p>{result.tournamentsRecorded} turneringer registrert.</p>
          {result.playersAdded.length > 0 && <p>Nye spillere: {result.playersAdded.join(", ")}</p>}
          <p>{result.playersUpdated} spillere oppdatert.</p>
          {result.playersLeftSquad.length > 0 && <p>Forlot troppen: {result.playersLeftSquad.join(", ")}</p>}
        </div>
      )}
    </div>
  );
}
