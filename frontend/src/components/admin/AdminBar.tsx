import { signOutAdmin } from "../../lib/auth";
import { useAdminMode } from "../../lib/adminMode";

/** In the site header while logged in: the admin mode switch, server status and log out. */
export function AdminBar() {
  const { user, enabled, setEnabled, active, backend } = useAdminMode();
  if (!user) return null;

  const status = !active
    ? null
    : backend.status === "ready"
      ? { label: "Server klar", tone: "ok" }
      : backend.status === "failed"
        ? { label: "Server svarer ikke", tone: "error" }
        : { label: `Starter server… ${backend.elapsedSeconds} s`, tone: "waiting" };

  return (
    <div className="admin-bar">
      <label className="admin-switch">
        <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
        Admin
      </label>
      {status && (
        <span className={`admin-status tone-${status.tone}`} role="status">
          {status.label}
        </span>
      )}
      <button type="button" className="link-button" onClick={() => signOutAdmin()}>
        Logg ut
      </button>
    </div>
  );
}
