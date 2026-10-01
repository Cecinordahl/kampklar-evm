import { Link } from "react-router-dom";
import { formatDate, formatTime } from "../lib/format";
import type { Match, Team } from "../types/firestore";

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
  return (
    <li className="match">
      <MatchRowContent {...props} />
    </li>
  );
}

/** The cells of a match row, without the list item - admin mode wraps them with its controls. */
export function MatchRowContent({ match, teams, highlightTeamIds = [] }: { match: Match } & Omit<Props, "matches">) {
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
      <span className={`match-score${finished ? " is-final" : ""}`}>
        {finished ? `${match.homeGoals}–${match.awayGoals}` : "–"}
      </span>
      <span className="match-away">{teamLink(match.awayTeamId)}</span>
    </>
  );
}
