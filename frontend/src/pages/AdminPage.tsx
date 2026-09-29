import { signOutAdmin, useAdminAuth } from "../lib/auth";
import { useBackendWarmup } from "../lib/useBackendWarmup";
import { Loading } from "../components/Status";
import { BackendStatusBanner } from "../components/admin/BackendStatusBanner";
import { LoginForm } from "../components/admin/LoginForm";
import { MatchEntrySection } from "../components/admin/MatchEntrySection";
import { TeamRefreshSection } from "../components/admin/TeamRefreshSection";

export function AdminPage() {
  const { user, loading } = useAdminAuth();

  if (loading) return <Loading />;
  if (!user) return <LoginForm />;
  return <AdminView email={user.email} />;
}

/** Nothing that writes is shown until the backend has answered, so no save can hang on a cold start. */
function AdminView({ email }: { email: string | null }) {
  const backend = useBackendWarmup();

  return (
    <>
      <div className="page-title-row">
        <h1>Admin</h1>
        <button type="button" className="button button-secondary" onClick={() => signOutAdmin()}>
          Logg ut
        </button>
      </div>
      <p className="muted small">Innlogget som {email}</p>

      {backend.status === "ready" ? (
        <>
          <MatchEntrySection />
          <TeamRefreshSection />
        </>
      ) : (
        <BackendStatusBanner status={backend.status} elapsedSeconds={backend.elapsedSeconds} />
      )}
    </>
  );
}
