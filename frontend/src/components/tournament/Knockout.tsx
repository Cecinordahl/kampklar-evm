// Nations League knockout stage: the quarter-final ties with both legs, and the road to the final.
import { Link } from "react-router-dom";
import nl from "../../content/nations-league.json";
import { formatDate, formatTime } from "../../lib/format";
import { knockoutTeamName, quarterFinals, tieLabel, type QuarterFinal } from "../../lib/knockout";
import { useTeams } from "../../lib/useFirestore";
import { Flag } from "../Flag";

const ko = nl.knockout;

/** A team name, linked when the site has a page for that team. */
function TeamName({ teamId, linkable, highlight }: { teamId: string; linkable: Set<string>; highlight: boolean }) {
  const name = knockoutTeamName(teamId);
  const className = highlight ? "highlight-team" : undefined;
  return linkable.has(teamId) ? (
    <Link to={`/lag/${teamId}`} className={className}>
      {name}
    </Link>
  ) : (
    <span className={className}>{name}</span>
  );
}

function TieCard({ qf, linkable, highlightTeamIds }: { qf: QuarterFinal; linkable: Set<string>; highlightTeamIds: string[] }) {
  return (
    <li className="ko-tie">
      <p className="ko-tie-title">
        <Flag teamId={qf.home} />
        <TeamName teamId={qf.home} linkable={linkable} highlight={highlightTeamIds.includes(qf.home)} />
        <span className="muted">–</span>
        <TeamName teamId={qf.away} linkable={linkable} highlight={highlightTeamIds.includes(qf.away)} />
        <Flag teamId={qf.away} />
      </p>
      <ol className="ko-legs">
        {qf.legs.map((leg) => (
          <li key={leg.leg}>
            <span className="ko-leg-name">{leg.leg === 1 ? "1. kamp" : "Retur"}</span>
            <time dateTime={leg.kickoff.toISOString()}>
              {formatDate(leg.kickoff)} kl. {formatTime(leg.kickoff)}
            </time>
            <span className="muted">
              {knockoutTeamName(leg.homeTeamId)} hjemme{leg.venue ? ` · ${leg.venue}` : ""}
            </span>
          </li>
        ))}
      </ol>
    </li>
  );
}

/** The quarter-final ties; pass teamId to show only that team's tie. */
export function QuarterFinalList({ teamId, highlightTeamIds = [] }: { teamId?: string; highlightTeamIds?: string[] }) {
  const teams = useTeams();
  const linkable = new Set(teams.data.keys());
  const ties = teamId ? quarterFinals.filter((qf) => qf.home === teamId || qf.away === teamId) : quarterFinals;
  return (
    <ul className="ko-ties">
      {ties.map((qf) => (
        <TieCard key={qf.id} qf={qf} linkable={linkable} highlightTeamIds={teamId ? [teamId] : highlightTeamIds} />
      ))}
    </ul>
  );
}

/** Semi-final paths: which quarter-final winners meet, then the final. */
export function KnockoutBracket() {
  return (
    <figure className="ko-bracket" aria-label="Veien til finalen">
      <div className="ko-bracket-semis">
        {ko.semiFinals.map((sf, i) => (
          <div key={sf.id} className="ko-semi">
            <span className="ko-round">Semifinale {i + 1}</span>
            <span className="ko-slot">Vinner {tieLabel(sf.from[0])}</span>
            <span className="ko-vs muted">mot</span>
            <span className="ko-slot">Vinner {tieLabel(sf.from[1])}</span>
            <span className="muted small">{sf.when}</span>
          </div>
        ))}
      </div>
      <div className="flow-arrow" aria-hidden="true">
        ↓
      </div>
      <div className="ko-final">
        <span className="ko-round">Finale</span>
        <strong>{ko.final.when}</strong>
        <span className="muted small">{ko.final.note}</span>
      </div>
    </figure>
  );
}

export function KnockoutNotice() {
  return <p className="notice small">{ko.notice}</p>;
}
