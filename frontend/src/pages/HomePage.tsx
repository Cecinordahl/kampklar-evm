import { Link } from "react-router-dom";
import { FavoriteButton } from "../components/FavoriteButton";
import { MatchCard } from "../components/MatchCard";
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
      <h2 className="page-section-title" id="mine-lag">
        Mine lag
      </h2>
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

      <section className="section" id="mesterskapene">
        <h2>Mesterskapene</h2>
        <div className="card-grid">
          <Link to="/em" className="card link-card">
            <strong>EM 2028</strong>
            <span className="muted">Kvalik, sluttspillformat og alle EM-finaler siden 1960</span>
          </Link>
          <Link to="/vm" className="card link-card">
            <strong>VM 2030</strong>
            <span className="muted">Ny europeisk kvalik, sluttspillformat og alle VM-finaler siden 1930</span>
          </Link>
          <Link to="/nations-league" className="card link-card">
            <strong>Nations League</strong>
            <span className="muted">Nivåer, grupper, rangering og veien til EM og VM</span>
          </Link>
        </div>
      </section>
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
  const playedCount = teamMatches.filter((m) => m.status === "FINISHED").length;

  const headingId = `team-card-${team.id}`;

  return (
    <article className="dash-card" aria-labelledby={headingId}>
      <header className="dash-header">
        <div className="dash-badge">
          <img src="/nations-league.webp" alt="UEFA Nations League" width="192" height="300" />
        </div>
        <div className="dash-titles">
          {group && (
            <p className="dash-eyebrow">
              League {group.name.charAt(0)} • <Link to={`/gruppe/${group.id}`}>Gruppe {group.name}</Link>
            </p>
          )}
          <h2 id={headingId}>
            <Link to={`/lag/${team.id}`}>{team.name}</Link>
          </h2>
        </div>
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
          <section className="dash-panel" aria-label="Tabell">
            <div className="dash-panel-head">
              <h3>Tabell</h3>
              <GamesPlayed played={playedCount} total={teamMatches.length} />
            </div>
            <StandingsTable group={group} teams={teams} highlightTeamIds={[team.id]} variant="dash" />
          </section>

          {(lastResult || nextMatch) && (
            <>
              <h3 className="dash-section-title">Kamper</h3>
              {lastResult && <MatchCard match={lastResult} teams={teams} kind="last" highlightTeamId={team.id} />}
              {nextMatch && <MatchCard match={nextMatch} teams={teams} kind="next" highlightTeamId={team.id} />}
            </>
          )}
        </>
      )}
    </article>
  );
}

/** One dot per group game for the team: lit for each one played. */
function GamesPlayed({ played, total }: { played: number; total: number }) {
  if (total === 0) return null;
  const label = `${played} av ${total} kamper spilt`;
  return (
    <span className="dash-dots" role="img" aria-label={label} title={label}>
      {Array.from({ length: total }, (_, i) => (
        <i key={i} className={i < played ? "is-played" : undefined} />
      ))}
    </span>
  );
}
