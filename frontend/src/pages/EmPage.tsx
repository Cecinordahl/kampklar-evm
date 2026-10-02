import { Link } from "react-router-dom";
import em from "../content/em.json";
import {
  FinalsFormat,
  FinalsHistory,
  Flow,
  FormatHistory,
  KeyFacts,
  NorwayHistory,
  Sources,
  TitleCounts,
  formatDateRange,
} from "../components/tournament/TournamentParts";
import { formatIsoDate } from "../lib/format";

export function EmPage() {
  const { next } = em;
  const q = next.qualification;
  const directTotal = q.direct.reduce((sum, d) => sum + d.count, 0);

  return (
    <>
      <p className="eyebrow">Europamesterskapet</p>
      <h1>{next.name}</h1>
      <KeyFacts
        facts={[
          { label: "Vertsland", value: next.hosts.join(", ") },
          { label: "Når", value: formatDateRange(next.dates.start, next.dates.end) },
          { label: "Lag", value: next.teams },
          { label: "Finale", value: next.finalVenue },
          { label: "Arenaer", value: next.stadiumCount },
        ]}
      />

      <section className="section">
        <h2>Slik kvalifiserer lagene seg</h2>
        <p>
          Trekningen er {formatIsoDate(q.drawDate)} i {q.drawPlace}. {q.seeding}{" "}
          <Link to="/nations-league">Slik fungerer Nations League →</Link>
        </p>
        <Flow
          caption={`Veien til ${next.name}: ${directTotal} direkte, ${q.hostSlots.count} vertsplasser og ${q.playoffs.spots} via playoff.`}
          stages={[
            [{ label: "Nations League 2026/27", sub: "Avgjør seeding og gir playoff-plasser" }],
            [
              {
                label: `${q.groupStage.groups} kvalikgrupper`,
                sub: `${q.groupStage.teamsPerGroup} lag, hjemme og borte · ${q.groupStage.period}`,
              },
            ],
            [
              ...q.direct.map((d) => ({ label: `${d.count} direkte`, sub: d.label, tone: "qualified" as const })),
              { label: `${q.hostSlots.count} vertsplasser`, sub: "Beste verter som ikke er direkte", tone: "host" as const },
              { label: "Playoff", sub: `${q.playoffs.period} · ${q.playoffs.spots} plasser`, tone: "playoff" as const },
            ],
            [{ label: `${next.name}`, sub: `${next.teams} lag`, tone: "qualified" }],
          ]}
        />
        <ul className="fact-list">
          <li>{q.groupStage.hostRule}</li>
          <li>{q.hostSlots.rule}</li>
          <li>Playoff: {q.playoffs.participants}</li>
        </ul>

        <h3>Playoff avhenger av vertsnasjonene</h3>
        <table className="history">
          <thead>
            <tr>
              <th scope="col">Vertsplasser brukt</th>
              <th scope="col" className="num">Lag i playoff</th>
              <th scope="col" className="num">Plasser</th>
            </tr>
          </thead>
          <tbody>
            {q.playoffs.scenarios.map((s) => (
              <tr key={s.hostSlotsUsed}>
                <td>
                  {s.hostSlotsUsed}
                  {s.note && <div className="muted small">{s.note}</div>}
                </td>
                <td className="num">{s.teams}</td>
                <td className="num">{s.spots}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="muted small">{q.playoffs.note}</p>
      </section>

      <section className="section">
        <h2>Sluttspillet</h2>
        <FinalsFormat
          groups={next.finals.groups}
          teamsPerGroup={next.finals.teamsPerGroup}
          bestThirds={4}
          knockout={next.finals.knockout}
        />
      </section>

      <section className="section">
        <h2>Norge i EM</h2>
        <NorwayHistory entries={em.norway} />
      </section>

      <section className="section">
        <h2>Slik har formatet endret seg</h2>
        <FormatHistory periods={em.formatHistory} />
      </section>

      <section className="section">
        <h2>Alle EM-finaler</h2>
        <h3>Flest titler</h3>
        <TitleCounts finals={em.finalsHistory} />
        <FinalsHistory finals={em.finalsHistory} />
      </section>

      <Sources urls={em._meta.sources} generatedAt={em._meta.generatedAt} />
    </>
  );
}
