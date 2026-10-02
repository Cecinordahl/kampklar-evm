import { formatDate, formatTime } from "../lib/format";
import { newsSourcePage, useTeamNews } from "../lib/news";

/** "Siste nytt" on a team page; renders nothing for a team without a feed. */
export function TeamNews({ teamId }: { teamId: string }) {
  const { items } = useTeamNews(teamId);

  // News is a nice-to-have: while loading, or if the feed fails, the section is simply left out.
  if (items.length === 0) return null;

  return (
    <section className="section">
      <h2>Siste nytt</h2>
      <ul className="news-list">
        {items.map((item) => (
          <li key={item.link}>
            <a href={item.link} target="_blank" rel="noopener noreferrer" className="news-title">
              {item.title}
            </a>
            {item.teaser && <p className="news-teaser">{item.teaser}</p>}
            {item.published && (
              <time className="muted small" dateTime={item.published.toISOString()}>
                {formatDate(item.published)} {formatTime(item.published)}
              </time>
            )}
          </li>
        ))}
      </ul>
      <p className="muted small">
        Kilde:{" "}
        <a href={newsSourcePage(teamId) ?? "https://www.nrk.no/sport/"} target="_blank" rel="noopener noreferrer">
          NRK Sport
        </a>
      </p>
    </section>
  );
}
