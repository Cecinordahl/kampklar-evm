import { Link, Outlet, useLocation } from "react-router-dom";
import { Hero } from "./Hero";
import { ThemeToggle } from "./ThemeToggle";
import { AdminBar } from "./admin/AdminBar";
import { useAdminMode } from "../lib/adminMode";

export function Layout() {
  const { user } = useAdminMode();
  const home = useLocation().pathname === "/";
  return (
    <>
      <header className={`site-header${home ? " is-overlay" : ""}`}>
        <div className="container site-header-inner">
          <Link to="/" className="brand">
            Kampklar <span className="brand-accent">EVM</span>
          </Link>
          <div className="header-end">
            {user ? <AdminBar /> : !home && <span className="muted header-note">Nations League 2026/27</span>}
            <ThemeToggle />
          </div>
        </div>
      </header>
      {home && <Hero />}
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
