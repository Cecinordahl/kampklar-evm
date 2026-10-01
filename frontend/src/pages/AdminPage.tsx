import { useEffect } from "react";
import { Navigate } from "react-router-dom";
import { useAdminMode } from "../lib/adminMode";
import { Loading } from "../components/Status";
import { LoginForm } from "../components/admin/LoginForm";

/** Login only: once logged in, admin mode is switched on and the admin edits on the regular pages. */
export function AdminPage() {
  const { user, authLoading, setEnabled } = useAdminMode();
  const loggedIn = user !== null;

  useEffect(() => {
    if (loggedIn) setEnabled(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only on login
  }, [loggedIn]);

  if (authLoading) return <Loading />;
  return loggedIn ? <Navigate to="/" replace /> : <LoginForm />;
}
