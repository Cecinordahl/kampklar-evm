// Admin mode on the home page: every played game without a result, across all groups, in one
// list - so one press fetches them all with AI instead of visiting each group or team.
import { useAdminMode } from "../../lib/adminMode";
import { useAllMatches, useTeams } from "../../lib/useFirestore";
import { LoadError, Loading } from "../Status";
import { EditableMatchList } from "./EditableMatchList";

export function AllResultsPanel() {
  const admin = useAdminMode();
  const matches = useAllMatches(admin.active);
  const teams = useTeams();
  if (!admin.active) return null;

  const now = new Date();
  const pending = matches.data.filter((m) => m.kickoff < now && m.status !== "FINISHED");

  return (
    <section className="section">
      <h2>Resultater som mangler</h2>
      {matches.loading ? (
        <Loading />
      ) : matches.error ? (
        <LoadError error={matches.error} />
      ) : pending.length === 0 ? (
        <p className="muted">✨ Alle spilte kamper i alle grupper har resultat.</p>
      ) : (
        <EditableMatchList matches={pending} teams={teams.data} scopeLabel="alle grupper" />
      )}
    </section>
  );
}
