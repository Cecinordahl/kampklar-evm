// The landing page hero: full-bleed team photo under a transparent site header.
// The photo is decorative - the heading carries the meaning.
export function Hero() {
  return (
    <section className="hero">
      <img
        className="hero-media"
        src="/hero.webp"
        srcSet="/hero-sm.webp 900w, /hero.webp 1920w"
        sizes="100vw"
        alt=""
        fetchPriority="high"
      />
      <div className="container hero-inner">
        <p className="hero-label">Nations League 2026/27</p>
        <h1>
          Kampklar<span className="hero-dot">.</span>
        </h1>
        <div className="hero-bottom">
          <p className="hero-lead">Tabeller, kamper og tropper for Norge, Spania og Frankrike – samlet på ett sted.</p>
          <div className="hero-actions">
            <a href="#mine-lag" className="hero-cta hero-cta-primary">
              Mine lag
            </a>
            <a href="#mesterskapene" className="hero-cta hero-cta-ghost">
              Mesterskapene
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
