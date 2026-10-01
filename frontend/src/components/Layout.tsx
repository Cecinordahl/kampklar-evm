import { Link, Outlet } from "react-router-dom";
import { AdminBar } from "./admin/AdminBar";
import { useAdminMode } from "../lib/adminMode";

export function Layout() {
  const { user } = useAdminMode();
  return (
    <>
      <header className="site-header">
        <div className="container site-header-inner">
          <Link to="/" className="brand">
            Kampklar <span className="brand-accent">EVM</span>
          </Link>
          {user ? <AdminBar /> : <span className="muted header-note">Nations League 2026/27</span>}
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
      <footer className="site-footer container muted">
        Uoffisielt fanprosjekt. Ikke tilknyttet UEFA eller noe fotballforbund.
        {!user && (
          <>
            {" · "}
            <Link to="/admin" className="footer-admin-link">
              Admin
            </Link>
          </>
        )}
      </footer>
    </>
  );
}
