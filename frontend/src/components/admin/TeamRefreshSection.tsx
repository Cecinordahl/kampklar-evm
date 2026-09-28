import { useState } from "react";
import { refreshTeam, type TeamRefreshResult } from "../../lib/adminApi";
import { useTeams } from "../../lib/useFirestore";

/** "Oppdater lagdata": slow (1-3 min), so the button stays disabled and says so while it runs. */
export function TeamRefreshSection() {
  const teams = useTeams();
  const [teamId, setTeamId] = useState("");
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<TeamRefreshResult | null>(null);

  async function handleRefresh() {
    if (!teamId) return;
    setRefreshing(true);
    setError(null);
    setResult(null);
    try {
      setResult(await refreshTeam(teamId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ukjent feil.");
    } finally {
      setRefreshing(false);
    }
  }

  return (
    <section className="section admin-section">
      <h2>Oppdater lagdata</h2>
      <p className="muted small">
        Henter landslagssjef, tropp og turneringshistorikk fra nettet. Tar 1–3 minutter.
      </p>

      <div className="admin-score-row">
        <label className="admin-field">
          Lag
          <select
            value={teamId}
            onChange={(e) => {
              setTeamId(e.target.value);
              setResult(null);
            }}
          >
            <option value="">Velg lag…</option>
            {[...teams.data.values()].map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>
        </label>
        <button type="button" className="button" onClick={handleRefresh} disabled={!teamId || refreshing}>
          {refreshing ? "Henter… (1–3 min)" : "Oppdater lagdata"}
        </button>
      </div>

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}

      {result && (
        <div className="admin-refresh-result">
          {result.coach && (
            <p>
              Landslagssjef: {result.coach.name} ({result.coach.nationality})
            </p>
          )}
          <p>{result.tournamentsRecorded} turneringer registrert.</p>
          {result.playersAdded.length > 0 && <p>Nye spillere: {result.playersAdded.join(", ")}</p>}
          <p>{result.playersUpdated} spillere oppdatert.</p>
          {result.playersLeftSquad.length > 0 && <p>Forlot troppen: {result.playersLeftSquad.join(", ")}</p>}
        </div>
      )}
    </section>
  );
}
