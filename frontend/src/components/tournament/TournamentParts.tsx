// Building blocks shared by the EM and VM pages. Content comes from src/content/*.json.
import type { ReactNode } from "react";
import { formatIsoDate } from "../../lib/format";

export interface FinalEntry {
  year: number;
  host: string;
  winner: string;
  runnerUp: string;
  score: string;
  note?: string;
}

export interface FormatPeriod {
  period: string;
  teams: number;
  format: string;
}

export interface NorwayEntry {
  year: number;
  result: string;
  detail: string;
}

export function KeyFacts({ facts }: { facts: { label: string; value: ReactNode }[] }) {
  return (
    <dl className="key-facts">
      {facts.map(({ label, value }) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}

export function formatDateRange(start: string, end: string): string {
  return `${formatIsoDate(start).replace(/ \d{4}$/, "")} – ${formatIsoDate(end)}`;
}

export type FlowTone = "neutral" | "qualified" | "playoff" | "host";

export interface FlowBox {
  label: string;
  sub?: string;
  tone?: FlowTone;
}

/**
 * A top-to-bottom path diagram: each stage is a row of boxes, joined by arrows. HTML rather
 * than SVG so it reflows to phone width and picks up the theme colours.
 */
export function Flow({ stages, caption }: { stages: FlowBox[][]; caption: string }) {
  return (
    <figure className="flow" aria-label={caption}>
      {stages.map((boxes, i) => (
        <div key={i}>
          {i > 0 && (
            <div className="flow-arrow" aria-hidden="true">
              ↓
            </div>
          )}
          <div className="flow-row">
            {boxes.map((box) => (
              <div key={box.label} className={`flow-box tone-${box.tone ?? "neutral"}`}>
                <strong>{box.label}</strong>
                {box.sub && <span>{box.sub}</span>}
              </div>
            ))}
          </div>
        </div>
      ))}
      <figcaption className="muted small">{caption}</figcaption>
      <FlowLegend />
    </figure>
  );
}

function FlowLegend() {
  const items: { tone: FlowTone; label: string }[] = [
    { tone: "qualified", label: "Kvalifisert" },
    { tone: "playoff", label: "Playoff" },
    { tone: "host", label: "Vertsnasjon" },
  ];
  return (
    <ul className="flow-legend small muted">
      {items.map(({ tone, label }) => (
        <li key={tone}>
          <span className={`legend-swatch tone-${tone}`} aria-hidden="true" />
          {label}
        </li>
      ))}
    </ul>
  );
}

/**
 * Illustrates a finals tournament: the group tables (who goes through), then the knockout
 * rounds with how many teams are left in each.
 */
export function FinalsFormat({
  groups,
  teamsPerGroup,
  bestThirds,
  knockout,
}: {
  groups: number;
  teamsPerGroup: number;
  bestThirds: number;
  knockout: string[];
}) {
  const teamsInKnockout = groups * 2 + bestThirds;
  const groupLetters = Array.from({ length: groups }, (_, i) => String.fromCharCode(65 + i));
  return (
    <figure className="finals-format">
      <div className="mini-groups">
        {groupLetters.map((letter) => (
          <div key={letter} className="mini-group" aria-hidden="true">
            <span className="mini-group-name">{letter}</span>
            {Array.from({ length: teamsPerGroup }, (_, pos) => (
              <span key={pos} className={`mini-slot ${pos < 2 ? "tone-qualified" : pos === 2 ? "tone-playoff" : ""}`} />
            ))}
          </div>
        ))}
      </div>
      <p className="muted small">
        {groups} grupper à {teamsPerGroup}: nr. 1 og 2 går videre (grønn), og de {bestThirds} beste treerne (gul) fyller
        opp til {teamsInKnockout} lag.
      </p>
      <ol className="knockout-steps">
        {knockout.map((round, i) => (
          <li key={round}>
            <strong>{round}</strong>
            <span className="muted small">{teamsInKnockout / 2 ** i} lag</span>
          </li>
        ))}
        <li className="champion">
          <strong>Mester</strong>
        </li>
      </ol>
    </figure>
  );
}

export function FormatHistory({ periods }: { periods: FormatPeriod[] }) {
  return (
    <table className="history">
      <thead>
        <tr>
          <th scope="col">Periode</th>
          <th scope="col" className="num">Lag</th>
          <th scope="col">Format</th>
        </tr>
      </thead>
      <tbody>
        {periods.map((p) => (
          <tr key={p.period}>
            <td className="nowrap">{p.period}</td>
            <td className="num">{p.teams}</td>
            <td>{p.format}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

// FIFA and UEFA count West Germany's titles as Germany's.
const SAME_NATION: Record<string, string> = { "Vest-Tyskland": "Tyskland" };

export function TitleCounts({ finals }: { finals: FinalEntry[] }) {
  const counts = new Map<string, number>();
  for (const f of finals) {
    const nation = SAME_NATION[f.winner] ?? f.winner;
    counts.set(nation, (counts.get(nation) ?? 0) + 1);
  }
  const sorted = [...counts].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], "nb"));
  return (
    <>
      <ul className="title-counts">
        {sorted.map(([nation, count]) => (
          <li key={nation}>
            <span className="title-count">{count}</span> {nation}
          </li>
        ))}
      </ul>
      <p className="muted small">Vest-Tysklands titler er regnet med under Tyskland.</p>
    </>
  );
}

export function FinalsHistory({ finals }: { finals: FinalEntry[] }) {
  return (
    <table className="history">
      <thead>
        <tr>
          <th scope="col">År</th>
          <th scope="col">Mester</th>
          <th scope="col">Finale</th>
          <th scope="col">Tapende finalist</th>
          <th scope="col" className="wide-only">Vertsland</th>
        </tr>
      </thead>
      <tbody>
        {[...finals].reverse().map((f) => (
          <tr key={f.year}>
            <td>{f.year}</td>
            <td>
              <strong>{f.winner}</strong>
              <div className="muted small narrow-only">{f.host}</div>
            </td>
            <td className="small">
              {f.score}
              {f.note && <div className="muted">{f.note}</div>}
            </td>
            <td>{f.runnerUp}</td>
            <td className="wide-only muted">{f.host}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export function NorwayHistory({ entries }: { entries: NorwayEntry[] }) {
  return (
    <ul className="norway-history">
      {entries.map((e) => (
        <li key={e.year}>
          <strong>
            {e.year}: {e.result}
          </strong>
          <span className="muted"> – {e.detail}</span>
        </li>
      ))}
    </ul>
  );
}

export function Sources({ urls, generatedAt }: { urls: string[]; generatedAt: string }) {
  return (
    <details className="sources small muted">
      <summary>Kilder (sist sjekket {formatIsoDate(generatedAt)})</summary>
      <ul>
        {urls.map((url) => (
          <li key={url}>
            <a href={url} target="_blank" rel="noreferrer">
              {url.replace(/^https:\/\/(www\.)?/, "")}
            </a>
          </li>
        ))}
      </ul>
    </details>
  );
}
