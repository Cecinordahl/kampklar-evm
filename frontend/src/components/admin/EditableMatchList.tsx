// The match list in admin mode: each match can be edited by hand or researched with AI, and the
// missing results among the listed matches can be researched in one go. Suggestions are only
// saved when the admin presses Lagre (one match) or "Lagre alle".
import { useState } from "react";
import { saveMatch, suggestResults, type ResultSuggestion } from "../../lib/adminApi";
import { useAdminMode } from "../../lib/adminMode";
import { daysBetween } from "../../lib/format";
import { MatchRowContent } from "../MatchList";
import { LongTaskOverlay } from "./LongTaskOverlay";
import { MatchForm, saveRequest } from "./MatchForm";
import type { Match, Team } from "../../types/firestore";

interface Props {
  matches: Match[];
  teams: Map<string, Team>;
  highlightTeamIds?: string[];
  /** Names the bulk button's scope, e.g. "gruppen" or "Frankrike". */
  scopeLabel: string;
}

export function EditableMatchList({ matches, teams, highlightTeamIds = [], scopeLabel }: Props) {
  const { ready } = useAdminMode();
  const [openMatchId, setOpenMatchId] = useState<string | null>(null);
  const [suggestions, setSuggestions] = useState<Map<string, ResultSuggestion>>(new Map());
  const [fetching, setFetching] = useState<string | null>(null);
  const [savingAll, setSavingAll] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const now = new Date();
  const played = (m: Match) => m.kickoff < now;
  // Nothing to enter before match day (Norwegian time): lineups and results don't exist yet.
  const matchDayReached = (m: Match) => daysBetween(now, m.kickoff) <= 0;
  const missing = matches.filter((m) => played(m) && m.status !== "FINISHED");
  const name = (teamId: string) => teams.get(teamId)?.name ?? teamId;
  const busy = fetching !== null || savingAll;

  /** Researches the given matches (one call per group) and merges the answers into the suggestions. */
  async function research(targets: Match[], title: string) {
    setFetching(title);
    setError(null);
    setMessage(null);
    try {
      const idsByGroup = new Map<string, string[]>();
      for (const m of targets) idsByGroup.set(m.groupId, [...(idsByGroup.get(m.groupId) ?? []), m.id]);
      const found: ResultSuggestion[] = [];
      for (const [groupId, ids] of idsByGroup) {
        found.push(...(await suggestResults(groupId, ids)));
      }
      setSuggestions((prev) => new Map([...prev, ...found.map((s) => [s.matchId, s] as const)]));
      const notFound = targets.length - found.length;
      setMessage(
        `${found.length} forslag hentet${notFound > 0 ? `, ${notFound} uten funn (ikke ferdigspilt eller ikke funnet)` : ""}.`,
      );
      if (targets.length === 1 && found.length === 1) setOpenMatchId(targets[0].id);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ukjent feil.");
    } finally {
      setFetching(null);
    }
  }

  async function handleSaveAll() {
    setSavingAll(true);
    setError(null);
    setMessage(null);
    const failures: string[] = [];
    let saved = 0;
    // One at a time: each save recomputes the same group's table in a transaction.
    for (const suggestion of suggestions.values()) {
      const match = matches.find((m) => m.id === suggestion.matchId);
      if (!match) continue;
      try {
        await saveMatch(match.id, saveRequest(match, "FINISHED", suggestion));
        saved++;
      } catch (err) {
        failures.push(`${name(match.homeTeamId)}–${name(match.awayTeamId)}: ${err instanceof Error ? err.message : "ukjent feil"}`);
      }
    }
    setSuggestions(new Map());
    setMessage(`${saved} kamper lagret. Tabellen er oppdatert.`);
    if (failures.length > 0) setError(`Feilet: ${failures.join("; ")}`);
    setSavingAll(false);
  }

  if (matches.length === 0) {
    return <p className="muted">Ingen kamper registrert ennå.</p>;
  }

  const notReady = ready ? undefined : "Venter på serveren…";
  const warningCount = [...suggestions.values()].reduce((n, s) => n + s.warnings.length, 0);

  return (
    <div className="editable-matches">
      <div className="admin-toolbar">
        <button
          type="button"
          className="button button-secondary button-small"
          disabled={!ready || busy || missing.length === 0}
          title={notReady}
          onClick={() => research(missing, `Henter manglende resultater for ${scopeLabel}`)}
        >
          ✨{" "}
          {missing.length === 0
            ? "Ingen manglende resultater"
            : `Hent ${missing.length === 1 ? "manglende resultat" : `${missing.length} manglende resultater`} med AI`}
        </button>
        {suggestions.size > 0 && (
          <button type="button" className="button button-small" disabled={!ready || busy} onClick={handleSaveAll}>
            {savingAll ? "Lagrer…" : `Lagre alle ${suggestions.size} forslag`}
          </button>
        )}
      </div>
      {warningCount > 0 && (
        <p className="small">{warningCount} advarsler – åpne kampene merket ⚠ og sjekk før du lagrer.</p>
      )}
      {message && (
        <p className="admin-success small" role="status">
          {message}
        </p>
      )}
      {error && (
        <p className="error small" role="alert">
          {error}
        </p>
      )}
      {fetching && <LongTaskOverlay title={fetching} />}

      <ol className="match-list">
        {matches.map((m) => {
          const suggestion = suggestions.get(m.id);
          const open = openMatchId === m.id;
          return (
            <li key={m.id} className="editable-match">
              <div className="match">
                <MatchRowContent match={m} teams={teams} highlightTeamIds={highlightTeamIds} />
              </div>
              {(suggestion || matchDayReached(m)) && (
                <div className="match-admin-actions">
                  {suggestion && (
                    <span className="admin-ai-tag">
                      AI: {suggestion.homeGoals}–{suggestion.awayGoals}
                      {suggestion.warnings.length > 0 ? " ⚠" : ""}
                    </span>
                  )}
                  {matchDayReached(m) && (
                    <button
                      type="button"
                      className="link-button"
                      aria-expanded={open}
                      onClick={() => setOpenMatchId(open ? null : m.id)}
                    >
                      ✏️ {open ? "Lukk" : "Manuelt"}
                    </button>
                  )}
                  {played(m) && (
                    <button
                      type="button"
                      className="link-button"
                      disabled={!ready || busy}
                      title={notReady}
                      onClick={() => research([m], `Henter ${name(m.homeTeamId)} – ${name(m.awayTeamId)} med AI`)}
                    >
                      ✨ AI
                    </button>
                  )}
                </div>
              )}
              {open && (
                <MatchForm key={`${m.id}-${suggestion ? "ai" : "stored"}`} match={m} suggestion={suggestion} teams={teams} />
              )}
            </li>
          );
        })}
      </ol>
    </div>
  );
}
