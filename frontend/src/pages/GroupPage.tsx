import { useParams } from "react-router-dom";
import { MatchList } from "../components/MatchList";
import { StandingsTable } from "../components/StandingsTable";
import { LoadError, Loading } from "../components/Status";
import { useFavorites } from "../lib/useFavorites";
import { useGroup, useGroupMatches, useTeams } from "../lib/useFirestore";

export function GroupPage() {
  const { groupId } = useParams();
  const group = useGroup(groupId);
  const matches = useGroupMatches(groupId);
  const teams = useTeams();
  const { favoriteIds } = useFavorites();

  if (group.loading) return <Loading />;
  if (group.error) return <LoadError error={group.error} />;
  if (!group.data) return <p>Fant ikke gruppen.</p>;

  return (
    <>
      <p className="eyebrow">Nations League 2026/27 · Liga A</p>
      <h1>Gruppe {group.data.name}</h1>

      <section className="section">
        <h2>Tabell</h2>
        <StandingsTable group={group.data} teams={teams.data} highlightTeamIds={favoriteIds} />
        <p className="muted small">
          Ved poenglikhet avgjør innbyrdes oppgjør før målforskjell, etter UEFAs regler.
        </p>
      </section>

      <section className="section">
        <h2>Kamper</h2>
        {matches.error ? (
          <LoadError error={matches.error} />
        ) : (
          <MatchList matches={matches.data} teams={teams.data} highlightTeamIds={favoriteIds} />
        )}
      </section>
    </>
  );
}
