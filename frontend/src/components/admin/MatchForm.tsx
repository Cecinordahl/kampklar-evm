// The manual result form, opened inline under a match in admin mode. An AI suggestion, when
// given, prefills it for review; saving goes through PUT /admin/matches, which updates the
// group table and players' caps/goals in one transaction.
import { useState, type FormEvent } from "react";
import { saveMatch, type MatchSaveRequest, type ResultSuggestion } from "../../lib/adminApi";
import { formatDate, formatTime } from "../../lib/format";
import { useSquad, type Live } from "../../lib/useFirestore";
import { LoadError, Loading } from "../Status";
import type { Match, MatchStatus, Player, Team } from "../../types/firestore";

export function saveRequest(
  match: Match,
  status: MatchStatus,
  result: Omit<ResultSuggestion, "matchId" | "warnings">,
): MatchSaveRequest {
  const finished = status === "FINISHED";
  return {
    competitionId: match.competitionId,
    groupId: match.groupId,
    homeTeamId: match.homeTeamId,
    awayTeamId: match.awayTeamId,
    kickoff: match.kickoff.toISOString(),
    status,
    homeGoals: finished ? result.homeGoals : null,
    awayGoals: finished ? result.awayGoals : null,
    homeLineup: finished ? result.homeLineup : [],
    awayLineup: finished ? result.awayLineup : [],
    homeScorerIds: finished ? result.homeScorerIds : [],
    awayScorerIds: finished ? result.awayScorerIds : [],
  };
}

function tally(scorerIds: string[]): Record<string, number> {
  const counts: Record<string, number> = {};
  for (const id of scorerIds) counts[id] = (counts[id] ?? 0) + 1;
  return counts;
}

function expandScorers(goalsByPlayer: Record<string, number>): string[] {
  return Object.entries(goalsByPlayer).flatMap(([id, count]) => Array(count).fill(id) as string[]);
}

export function MatchForm({
  match,
  suggestion,
  teams,
}: {
  match: Match;
  suggestion: ResultSuggestion | undefined;
  teams: Map<string, Team>;
}) {
  const homeSquad = useSquad(match.homeTeamId);
  const awaySquad = useSquad(match.awayTeamId);
  // A suggestion, when present, replaces what is stored; the admin reviews it before saving.
  const initial = suggestion ?? match;

  const [status, setStatus] = useState<MatchStatus>(suggestion ? "FINISHED" : match.status);
  const [homeGoals, setHomeGoals] = useState(initial.homeGoals ?? 0);
  const [awayGoals, setAwayGoals] = useState(initial.awayGoals ?? 0);
  const [homeLineup, setHomeLineup] = useState<Set<string>>(new Set(initial.homeLineup));
  const [awayLineup, setAwayLineup] = useState<Set<string>>(new Set(initial.awayLineup));
  const [homeGoalsByPlayer, setHomeGoalsByPlayer] = useState<Record<string, number>>(() =>
    tally(initial.homeScorerIds),
  );
  const [awayGoalsByPlayer, setAwayGoalsByPlayer] = useState<Record<string, number>>(() =>
    tally(initial.awayScorerIds),
  );
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  function togglePlayer(side: "home" | "away", playerId: string, playing: boolean) {
    const setLineup = side === "home" ? setHomeLineup : setAwayLineup;
    const setGoalsByPlayer = side === "home" ? setHomeGoalsByPlayer : setAwayGoalsByPlayer;
    setLineup((prev) => {
      const next = new Set(prev);
      if (playing) next.add(playerId);
      else next.delete(playerId);
      return next;
    });
    if (!playing) {
      setGoalsByPlayer((prev) => {
        const rest = { ...prev };
        delete rest[playerId];
        return rest;
      });
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setSaved(false);
    const body = saveRequest(match, status, {
      homeGoals,
      awayGoals,
      homeLineup: [...homeLineup],
      awayLineup: [...awayLineup],
      homeScorerIds: expandScorers(homeGoalsByPlayer),
      awayScorerIds: expandScorers(awayGoalsByPlayer),
    });
    try {
      await saveMatch(match.id, body);
      setSaved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ukjent feil.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="admin-form admin-match-form" onSubmit={handleSubmit}>
      <p className="admin-match-title">
        {teams.get(match.homeTeamId)?.name ?? match.homeTeamId} – {teams.get(match.awayTeamId)?.name ?? match.awayTeamId}
        <span className="muted">
          {" "}
          · {formatDate(match.kickoff)} {formatTime(match.kickoff)}
        </span>
      </p>

      {suggestion && (
        <div className="admin-ai-note">
          <p className="small">Forhåndsutfylt av AI – sjekk før du lagrer.</p>
          {suggestion.warnings.length > 0 && (
            <ul className="small">
              {suggestion.warnings.map((w) => (
                <li key={w}>{w}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      <label className="admin-field">
        Status
        <select value={status} onChange={(e) => setStatus(e.target.value as MatchStatus)}>
          <option value="SCHEDULED">Kommer</option>
          <option value="FINISHED">Ferdigspilt</option>
        </select>
      </label>

      {status === "FINISHED" && (
        <>
          <div className="admin-score-row">
            <label className="admin-field admin-field-narrow">
              {teams.get(match.homeTeamId)?.name ?? "Hjemme"}
              <input
                type="number"
                min={0}
                value={homeGoals}
                onChange={(e) => setHomeGoals(Number(e.target.value))}
                required
              />
            </label>
            <label className="admin-field admin-field-narrow">
              {teams.get(match.awayTeamId)?.name ?? "Borte"}
              <input
                type="number"
                min={0}
                value={awayGoals}
                onChange={(e) => setAwayGoals(Number(e.target.value))}
                required
              />
            </label>
          </div>

          <SquadPicker
            label={`Tropp ${teams.get(match.homeTeamId)?.name ?? ""}`}
            squad={homeSquad}
            lineup={homeLineup}
            goalsByPlayer={homeGoalsByPlayer}
            onToggle={(id, playing) => togglePlayer("home", id, playing)}
            onGoalsChange={(id, count) => setHomeGoalsByPlayer((prev) => ({ ...prev, [id]: count }))}
          />
          <SquadPicker
            label={`Tropp ${teams.get(match.awayTeamId)?.name ?? ""}`}
            squad={awaySquad}
            lineup={awayLineup}
            goalsByPlayer={awayGoalsByPlayer}
            onToggle={(id, playing) => togglePlayer("away", id, playing)}
            onGoalsChange={(id, count) => setAwayGoalsByPlayer((prev) => ({ ...prev, [id]: count }))}
          />
        </>
      )}

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {saved && (
        <p className="admin-success" role="status">
          Lagret. Tabellen er oppdatert.
        </p>
      )}

      <button type="submit" className="button" disabled={saving}>
        {saving ? "Lagrer…" : "Lagre"}
      </button>
    </form>
  );
}

function SquadPicker({
  label,
  squad,
  lineup,
  goalsByPlayer,
  onToggle,
  onGoalsChange,
}: {
  label: string;
  squad: Live<Player[]>;
  lineup: Set<string>;
  goalsByPlayer: Record<string, number>;
  onToggle: (playerId: string, playing: boolean) => void;
  onGoalsChange: (playerId: string, count: number) => void;
}) {
  if (squad.loading) return <Loading />;
  if (squad.error) return <LoadError error={squad.error} />;

  return (
    <fieldset className="admin-squad">
      <legend>{label}</legend>
      {squad.data.length === 0 ? (
        <p className="muted small">Ingen tropp registrert ennå.</p>
      ) : (
        <ul className="admin-squad-list">
          {squad.data.map((p) => (
            <li key={p.id} className="admin-squad-row">
              <label>
                <input type="checkbox" checked={lineup.has(p.id)} onChange={(e) => onToggle(p.id, e.target.checked)} />
                {p.name}
              </label>
              {lineup.has(p.id) && (
                <label className="admin-goals-input">
                  Mål
                  <input
                    type="number"
                    min={0}
                    value={goalsByPlayer[p.id] ?? 0}
                    onChange={(e) => onGoalsChange(p.id, Number(e.target.value))}
                  />
                </label>
              )}
            </li>
          ))}
        </ul>
      )}
    </fieldset>
  );
}
