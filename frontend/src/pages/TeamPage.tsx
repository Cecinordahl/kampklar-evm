import { Link, useParams } from "react-router-dom";
import { FavoriteButton } from "../components/FavoriteButton";
import { MatchList } from "../components/MatchList";
import { LoadError, Loading } from "../components/Status";
import { useGroupMatches, useGroupsForTeam, useSquad, useTeam, useTeams } from "../lib/useFirestore";
import type { Player } from "../types/firestore";

const POSITIONS: { code: string; label: string }[] = [
  { code: "GK", label: "Keepere" },
  { code: "DF", label: "Forsvarsspillere" },
  { code: "MF", label: "Midtbanespillere" },
  { code: "FW", label: "Angripere" },
];

export function TeamPage() {
  const { teamId } = useParams();
  const team = useTeam(teamId);
  const teams = useTeams();
  const groups = useGroupsForTeam(teamId);
  const group = groups.data[0];
  const matches = useGroupMatches(group?.id);
  const squad = useSquad(teamId);

  if (team.loading) return <Loading />;
  if (team.error) return <LoadError error={team.error} />;
  if (!team.data || !teamId) return <p>Fant ikke laget.</p>;

  const teamMatches = matches.data.filter((m) => m.homeTeamId === teamId || m.awayTeamId === teamId);
  const { coach, tournamentHistory, refreshedAt } = team.data;

  return (
    <>
      <div className="page-title-row">
        <h1>{team.data.name}</h1>
        <FavoriteButton teamId={teamId} teamName={team.data.name} />
      </div>
      <p className="lead">
        {coach ? <>Landslagssjef: {coach.name} ({coach.nationality})</> : "Lagdata er ikke hentet ennå."}
        {group && (
          <>
            {" · "}
            <Link to={`/gruppe/${group.id}`}>Gruppe {group.name}</Link>
          </>
        )}
      </p>

      <section className="section">
        <h2>Kamper</h2>
        {matches.error ? (
          <LoadError error={matches.error} />
        ) : (
          <MatchList matches={teamMatches} teams={teams.data} highlightTeamIds={[teamId]} />
        )}
      </section>

      <section className="section">
        <h2>Tropp</h2>
        {squad.loading ? (
          <Loading />
        ) : squad.error ? (
          <LoadError error={squad.error} />
        ) : squad.data.length === 0 ? (
          <p className="muted">Troppen er ikke hentet ennå.</p>
        ) : (
          <div className="squad">
            {POSITIONS.map(({ code, label }) => (
              <SquadGroup key={code} label={label} players={squad.data.filter((p) => p.position === code)} />
            ))}
            <SquadGroup
              label="Annet"
              players={squad.data.filter((p) => !POSITIONS.some(({ code }) => code === p.position))}
            />
          </div>
        )}
      </section>

      {tournamentHistory.length > 0 && (
        <section className="section">
          <h2>EM og VM siden 2000</h2>
          <table className="history">
            <thead>
              <tr>
                <th scope="col">År</th>
                <th scope="col">Turnering</th>
                <th scope="col">Resultat</th>
              </tr>
            </thead>
            <tbody>
              {[...tournamentHistory]
                .sort((a, b) => b.year - a.year)
                .map((entry) => (
                  <tr key={`${entry.tournament}-${entry.year}`}>
                    <td>{entry.year}</td>
                    <td>{entry.tournament}</td>
                    <td>{entry.result}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </section>
      )}

      {refreshedAt && (
        <p className="muted small">
          Lagdata sist oppdatert {refreshedAt.toLocaleDateString("nb-NO", { timeZone: "Europe/Oslo" })}.
          Tropp og statistikk er hentet automatisk fra offentlige kilder og kan inneholde feil.
        </p>
      )}
    </>
  );
}

function SquadGroup({ label, players }: { label: string; players: Player[] }) {
  if (players.length === 0) return null;
  return (
    <div className="squad-group">
      <h3>{label}</h3>
      <table className="squad-table">
        <thead>
          <tr>
            <th scope="col">Navn</th>
            <th scope="col" className="wide-only">Klubb</th>
            <th scope="col" className="num" title="Landskamper">Kamper</th>
            <th scope="col" className="num" title="Landslagsmål">Mål</th>
          </tr>
        </thead>
        <tbody>
          {[...players]
            .sort((a, b) => b.caps - a.caps)
            .map((p) => (
              <tr key={p.id}>
                <td>
                  {p.name}
                  {p.club && <span className="muted narrow-only"> · {p.club}</span>}
                </td>
                <td className="wide-only muted">{p.club}</td>
                <td className="num">{p.caps}</td>
                <td className="num">{p.goals}</td>
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
}
