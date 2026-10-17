import { useState } from "react";
import { Link } from "react-router-dom";
import { useAdminMode } from "../lib/adminMode";
import { formatDate, formatTime } from "../lib/format";
import { isResultMissing } from "../lib/matches";
import { MatchForm } from "./admin/MatchForm";
import type { Match, Team } from "../types/firestore";

// Where "Si fra" sends visitors; the button is hidden when it isn't configured.
const ADMIN_EMAIL = import.meta.env.VITE_ADMIN_EMAIL as string | undefined;

interface Props {
  matches: Match[];
  teams: Map<string, Team>;
  highlightTeamIds?: string[];
}

export function MatchList({ matches, teams, highlightTeamIds = [] }: Props) {
  if (matches.length === 0) {
    return <p className="muted">Ingen kamper registrert ennå.</p>;
  }
  return (
    <ol className="match-list">
      {matches.map((m) => (
        <MatchRow key={m.id} match={m} teams={teams} highlightTeamIds={highlightTeamIds} />
      ))}
    </ol>
  );
}

export function MatchRow(props: { match: Match } & Omit<Props, "matches">) {
  const { active: adminActive } = useAdminMode();
  const [formOpen, setFormOpen] = useState(false);
  const missing = isResultMissing(props.match);
  const editable = missing && adminActive;
  return (
    <li className={`match${missing ? " is-missing" : ""}${editable ? " has-edit-icon" : ""}`}>
      <MatchRowContent {...props} missingResult={missing} />
      {editable && (
        <button
          type="button"
          className="match-edit-icon"
          aria-expanded={formOpen}
          aria-label={formOpen ? "Lukk" : "Legg til resultat"}
          title={formOpen ? "Lukk" : "Legg til resultat"}
          onClick={() => setFormOpen(!formOpen)}
        >
          {formOpen ? "✕" : "✏️"}
        </button>
      )}
      {missing && !adminActive && <MissingResultNote match={props.match} teams={props.teams} />}
      {editable && formOpen && (
        <div className="match-inline-form">
          <MatchForm match={props.match} suggestion={undefined} teams={props.teams} onSaved={() => setFormOpen(false)} />
        </div>
      )}
    </li>
  );
}

/** The public "result not updated" line, with a prefilled email to the admin. */
export function MissingResultNote({ match, teams }: { match: Match; teams: Map<string, Team> }) {
  const title = `${teams.get(match.homeTeamId)?.name ?? match.homeTeamId} – ${teams.get(match.awayTeamId)?.name ?? match.awayTeamId}`;
  const subject = `Resultat mangler: ${title}`;
  const body = `Hei! Resultatet for ${title} (${formatDate(match.kickoff)} ${formatTime(match.kickoff)}) er ikke oppdatert på Kampklar.`;
  return (
    <p className="match-missing-note">
      <span>Resultatet er ikke oppdatert ennå.</span>
      {ADMIN_EMAIL && (
        <a
          className="match-missing-ping"
          href={`mailto:${ADMIN_EMAIL}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`}
        >
          ✉️ Si fra
        </a>
      )}
    </p>
  );
}

/** The cells of a match row, without the list item - admin mode wraps them with its controls. */
export function MatchRowContent({
  match,
  teams,
  highlightTeamIds = [],
  missingResult = false,
}: { match: Match; missingResult?: boolean } & Omit<Props, "matches">) {
  const finished = match.status === "FINISHED";
  const teamLink = (teamId: string) => (
    <Link to={`/lag/${teamId}`} className={highlightTeamIds.includes(teamId) ? "highlight-team" : undefined}>
      {teams.get(teamId)?.name ?? teamId}
    </Link>
  );

  return (
    <>
      <time className="match-when" dateTime={match.kickoff.toISOString()}>
        <span>{formatDate(match.kickoff)}</span>
        {!finished && <span className="muted">{formatTime(match.kickoff)}</span>}
      </time>
      <span className="match-home">{teamLink(match.homeTeamId)}</span>
      <span className={`match-score${finished ? " is-final" : ""}${missingResult ? " is-missing" : ""}`}>
        {finished ? `${match.homeGoals}–${match.awayGoals}` : missingResult ? "?" : "–"}
      </span>
      <span className="match-away">{teamLink(match.awayTeamId)}</span>
    </>
  );
}
