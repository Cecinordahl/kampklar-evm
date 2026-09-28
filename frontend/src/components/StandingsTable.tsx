import { Link } from "react-router-dom";
import { formatGoalDifference } from "../lib/format";
import type { Group, Team } from "../types/firestore";

interface Props {
  group: Group;
  teams: Map<string, Team>;
  highlightTeamIds?: string[];
}

export function StandingsTable({ group, teams, highlightTeamIds = [] }: Props) {
  return (
    <table className="standings">
      <caption className="visually-hidden">Tabell {group.name}</caption>
      <thead>
        <tr>
          <th scope="col" className="num">#</th>
          <th scope="col" className="team-col">Lag</th>
          <th scope="col" className="num" title="Kamper">K</th>
          <th scope="col" className="num" title="Vunnet">V</th>
          <th scope="col" className="num" title="Uavgjort">U</th>
          <th scope="col" className="num" title="Tap">T</th>
          <th scope="col" className="num wide-only" title="Mål for og mot">Mål</th>
          <th scope="col" className="num" title="Målforskjell">+/-</th>
          <th scope="col" className="num" title="Poeng">P</th>
        </tr>
      </thead>
      <tbody>
        {group.standings.map((s) => (
          <tr key={s.teamId} className={highlightTeamIds.includes(s.teamId) ? "highlight" : undefined}>
            <td className="num">{s.rank}</td>
            <td className="team-col">
              <Link to={`/lag/${s.teamId}`}>{teams.get(s.teamId)?.name ?? s.teamId}</Link>
            </td>
            <td className="num">{s.played}</td>
            <td className="num">{s.won}</td>
            <td className="num">{s.drawn}</td>
            <td className="num">{s.lost}</td>
            <td className="num wide-only">
              {s.goalsFor}–{s.goalsAgainst}
            </td>
            <td className="num">{formatGoalDifference(s.goalDifference)}</td>
            <td className="num points">{s.points}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
