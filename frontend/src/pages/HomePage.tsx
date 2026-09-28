import { Link } from "react-router-dom";
import { FavoriteButton } from "../components/FavoriteButton";
import { MatchRow } from "../components/MatchList";
import { StandingsTable } from "../components/StandingsTable";
import { LoadError, Loading } from "../components/Status";
import { useFavorites } from "../lib/useFavorites";
import { useGroupMatches, useGroupsForTeam, useTeams } from "../lib/useFirestore";
import { SUPPORTED_FAVORITE_TEAMS, type FavoriteTeam } from "../types/team";
import type { Team } from "../types/firestore";

export function HomePage() {
  const { favoriteIds } = useFavorites();
  const teams = useTeams();
  const followed = SUPPORTED_FAVORITE_TEAMS.filter((t) => favoriteIds.includes(t.id));
  const notFollowed = SUPPORTED_FAVORITE_TEAMS.filter((t) => !favoriteIds.includes(t.id));

  return (
    <>
      <h1>Mine lag</h1>
      {teams.error && <LoadError error={teams.error} />}

      {followed.length === 0 ? (
        <p className="lead">Velg lagene du vil følge, så får du tabell og neste kamp samlet her.</p>
      ) : (
        <div className="card-grid">
          {followed.map((team) => (
            <TeamCard key={team.id} team={team} teams={teams.data} />
          ))}
        </div>
      )}

      {notFollowed.length > 0 && (
        <section className="section">
          <h2>{followed.length === 0 ? "Velg lag" : "Følg flere lag"}</h2>
          <ul className="chip-list">
            {notFollowed.map((team) => (
              <li key={team.id} className="chip">
                <Link to={`/lag/${team.id}`}>{team.name}</Link>
                <FavoriteButton teamId={team.id} teamName={team.name} />
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}

function TeamCard({ team, teams }: { team: FavoriteTeam; teams: Map<string, Team> }) {
  const groups = useGroupsForTeam(team.id);
  const group = groups.data[0];
  const matches = useGroupMatches(group?.id);

  const teamMatches = matches.data.filter((m) => m.homeTeamId === team.id || m.awayTeamId === team.id);
  const lastResult = teamMatches.filter((m) => m.status === "FINISHED").at(-1);
  const nextMatch = teamMatches.find((m) => m.status === "SCHEDULED");

  return (
    <article className="card">
      <header className="card-header">
        <h2>
          <Link to={`/lag/${team.id}`}>{team.name}</Link>
        </h2>
        <FavoriteButton teamId={team.id} teamName={team.name} />
      </header>

      {groups.loading ? (
        <Loading />
      ) : groups.error ? (
        <LoadError error={groups.error} />
      ) : !group ? (
        <p className="muted">Ingen gruppe registrert ennå.</p>
      ) : (
        <>
          <p className="card-subtitle">
            <Link to={`/gruppe/${group.id}`}>Gruppe {group.name}</Link>
          </p>
          <StandingsTable group={group} teams={teams} highlightTeamIds={[team.id]} />
          <dl className="fixtures-summary">
            {lastResult && (
              <div>
                <dt>Siste kamp</dt>
                <dd>
                  <ol className="match-list">
                    <MatchRow match={lastResult} teams={teams} highlightTeamIds={[team.id]} />
                  </ol>
                </dd>
              </div>
            )}
            {nextMatch && (
              <div>
                <dt>Neste kamp</dt>
                <dd>
                  <ol className="match-list">
                    <MatchRow match={nextMatch} teams={teams} highlightTeamIds={[team.id]} />
                  </ol>
                </dd>
              </div>
            )}
          </dl>
        </>
      )}
    </article>
  );
}
