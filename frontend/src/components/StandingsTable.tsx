import { Link } from "react-router-dom";
import { formatGoalDifference } from "../lib/format";
import { Flag } from "./Flag";
import type { Group, Team } from "../types/firestore";

interface Props {
  group: Group;
  teams: Map<string, Team>;
  highlightTeamIds?: string[];
  /** The compact dashboard look on the home page: no goals column, quarter-final marker. */
  variant?: "default" | "dash";
}

// In League A the top two of each group go on to the quarter-finals.
const QUARTER_FINAL_PLACES = 2;

export function StandingsTable({ group, teams, highlightTeamIds = [], variant = "default" }: Props) {
  const dash = variant === "dash";
  const qualifyRows = Math.min(QUARTER_FINAL_PLACES, group.standings.length);
  const table = (
    <table className={`standings${dash ? " standings-dash" : ""}`}>
      <caption className="visually-hidden">Tabell {group.name}</caption>
      <thead>
        <tr>
          {dash && <th scope="col" className="qual-col" aria-hidden="true" />}
          <th scope="col" className="num">#</th>
          <th scope="col" className="team-col">Lag</th>
          <th scope="col" className="num" title="Kamper">K</th>
          <th scope="col" className="num" title="Vunnet">V</th>
          <th scope="col" className="num" title="Uavgjort">U</th>
          <th scope="col" className="num" title="Tap">T</th>
          {!dash && (
            <th scope="col" className="num wide-only" title="Mål for og mot">
              Mål
            </th>
          )}
          <th scope="col" className="num" title="Målforskjell">+/-</th>
          <th scope="col" className="num" title="Poeng">P</th>
        </tr>
      </thead>
      <tbody>
        {group.standings.map((s, i) => (
          <tr
            key={s.teamId}
            className={
              [highlightTeamIds.includes(s.teamId) && "highlight", dash && i < qualifyRows && "is-qualify"]
                .filter(Boolean)
                .join(" ") || undefined
            }
          >
            {dash &&
              (i === 0 ? (
                <td className="qual-cell" rowSpan={qualifyRows} />
              ) : i >= qualifyRows ? (
                <td className="qual-cell qual-empty" />
              ) : null)}
            <td className="num rank">{s.rank}</td>
            <td className="team-col">
              <Link to={`/lag/${s.teamId}`} className="team-link">
                <Flag teamId={s.teamId} />
                <span>{teams.get(s.teamId)?.name ?? s.teamId}</span>
              </Link>
            </td>
            <td className="num">{s.played}</td>
            <td className="num">{s.won}</td>
            <td className="num">{s.drawn}</td>
            <td className="num">{s.lost}</td>
            {!dash && (
              <td className="num wide-only">
                {s.goalsFor}–{s.goalsAgainst}
              </td>
            )}
            <td className="num">{formatGoalDifference(s.goalDifference)}</td>
            <td className="num points">{s.points}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
  if (!dash || qualifyRows === 0) return table;
  return (
    <>
      {table}
      <p className="qual-legend">
        <span className="qual-legend-bar" aria-hidden="true" />
        Topp {qualifyRows}: Nations League-kvartfinale
      </p>
    </>
  );
}
