import { Link, Outlet } from "react-router-dom";

export function Layout() {
  return (
    <>
      <header className="site-header">
        <div className="container site-header-inner">
          <Link to="/" className="brand">
            Kampklar <span className="brand-accent">EVM</span>
          </Link>
          <span className="muted header-note">Nations League 2026/27</span>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
      <footer className="site-footer container muted">
        Uoffisielt fanprosjekt. Ikke tilknyttet UEFA eller noe fotballforbund.
      </footer>
    </>
  );
}
