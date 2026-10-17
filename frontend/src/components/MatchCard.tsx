// A single match as a card on the home page dashboard: "Siste kamp" with the score,
// "Neste kamp" with a VS badge, or "Resultat mangler" for a game past kickoff + 2 h without a
// result - a "?" badge and, for the admin, a pencil that opens the result form in the card.
import { useState } from "react";
import { Link } from "react-router-dom";
import { useAdminMode } from "../lib/adminMode";
import { formatDate, formatTime } from "../lib/format";
import { isResultMissing } from "../lib/matches";
import { MatchForm } from "./admin/MatchForm";
import { Flag } from "./Flag";
import { MissingResultNote } from "./MatchList";
import type { Match, Team } from "../types/firestore";

interface Props {
  match: Match;
  teams: Map<string, Team>;
  kind: "last" | "next" | "missing";
  highlightTeamId?: string;
}

const TITLES = { last: "Siste kamp", next: "Neste kamp", missing: "Resultat mangler" } as const;

export function MatchCard({ match, teams, kind, highlightTeamId }: Props) {
  const { active: adminActive } = useAdminMode();
  const [formOpen, setFormOpen] = useState(false);
  const finished = match.status === "FINISHED";
  const missing = isResultMissing(match);
  const editable = missing && adminActive;
  const title = TITLES[kind];

  const team = (teamId: string, side: "home" | "away") => (
    <Link
      to={`/lag/${teamId}`}
      className={`mc-team mc-${side}${teamId === highlightTeamId ? " is-highlight" : ""}`}
    >
      {teams.get(teamId)?.name ?? teamId}
    </Link>
  );

  return (
    <section className={`match-card match-card-${kind}${missing ? " is-missing" : ""}`} aria-label={title}>
      <header className="match-card-head">
        <h4>{title}</h4>
        <time className="match-card-when" dateTime={match.kickoff.toISOString()}>
          {formatDate(match.kickoff)}
          {!finished && ` ${formatTime(match.kickoff)}`}
        </time>
        {editable && (
          <button
            type="button"
            className="match-card-edit"
            aria-expanded={formOpen}
            aria-label={formOpen ? "Lukk" : "Legg til resultat"}
            title={formOpen ? "Lukk" : "Legg til resultat"}
            onClick={() => setFormOpen(!formOpen)}
          >
            {formOpen ? (
              <span aria-hidden="true">✕</span>
            ) : (
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M11 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-5" />
                <path d="M18.4 2.6a2 2 0 0 1 2.9 2.9L12 14.8l-3.6.9.9-3.6 9.1-9.5z" />
              </svg>
            )}
          </button>
        )}
      </header>

      <div className="match-card-body">
        {team(match.homeTeamId, "home")}
        <Flag teamId={match.homeTeamId} size="lg" />
        {finished ? (
          <span className="mc-score">
            {match.homeGoals}
            <span className="mc-score-sep">-</span>
            {match.awayGoals}
          </span>
        ) : (
          <span className="mc-vs">{missing ? "?" : "VS"}</span>
        )}
        <Flag teamId={match.awayTeamId} size="lg" />
        {team(match.awayTeamId, "away")}
      </div>

      {missing && !adminActive && <MissingResultNote match={match} teams={teams} />}
      {editable && formOpen && (
        <div className="match-card-form">
          <MatchForm match={match} suggestion={undefined} teams={teams} onSaved={() => setFormOpen(false)} />
        </div>
      )}
    </section>
  );
}
