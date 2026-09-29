import vm from "../content/vm.json";
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

export function VmPage() {
  const { next } = vm;
  const q = next.qualification;
  const [league1, league2] = q.leagues;

  return (
    <>
      <p className="eyebrow">Verdensmesterskapet</p>
      <h1>{next.name}</h1>
      <KeyFacts
        facts={[
          { label: "Vertsland", value: next.hosts.join(", ") },
          { label: "Jubileumskamper", value: next.centenaryHosts.countries.join(", ") },
          { label: "Når", value: formatDateRange(next.dates.start, next.dates.end) },
          { label: "Lag", value: `${next.teams}*` },
          { label: "Europeiske plasser", value: `${next.uefaSlots}*` },
          { label: "Finale", value: next.finalVenue ?? "Ikke bestemt" },
        ]}
      />
      <p className="muted small">
        * {next.teamsNote} {next.uefaSlotsNote}
      </p>
      <p>{next.centenaryHosts.note}</p>

      <section className="section">
        <h2>Slik kvalifiserer europeiske lag seg</h2>
        <p className="notice">{q.status}</p>
        <p>{q.seeding}</p>
        <Flow
          caption="Den nye europeiske VM-kvaliken: to nivåer, direkte plasser fra nivå 1 og playoff for resten."
          stages={[
            [{ label: "Nations League 2028/29", sub: "Avgjør hvilken liga landet havner i" }],
            [
              { label: `${league1.name}: ${league1.teams} lag`, sub: "3 grupper à 12 · 6 kamper hver" },
              { label: `${league2.name}: ${league2.teams} lag`, sub: "3 grupper à 6 · hjemme og borte" },
            ],
            [
              { label: "Direkte", sub: "Best rangerte i hver Liga 1-gruppe", tone: "qualified" },
              { label: "Playoff", sub: "Resten av Liga 1 + beste fra Liga 2", tone: "playoff" },
              { label: "Spania og Portugal", sub: "Direkte som vertsnasjoner", tone: "host" },
            ],
            [{ label: next.name, sub: `${next.uefaSlots}* europeiske lag`, tone: "qualified" }],
          ]}
        />
        <ul className="fact-list">
          <li>
            <strong>{league1.name}:</strong> {league1.who}. {league1.format}
          </li>
          <li>
            <strong>{league2.name}:</strong> {league2.who}. {league2.format}
          </li>
          <li>{q.direct}</li>
          <li>{q.playoffs}</li>
          <li>{q.hosts}</li>
        </ul>
        <h3>Til sammenligning: {next.previousUefaQualification.edition}</h3>
        <p>{next.previousUefaQualification.format}</p>
      </section>

      <section className="section">
        <h2>Sluttspillet</h2>
        <FinalsFormat
          groups={next.finals.groups}
          teamsPerGroup={next.finals.teamsPerGroup}
          bestThirds={8}
          knockout={next.finals.knockout}
        />
        <p className="muted small">{next.finals.note}</p>
      </section>

      <section className="section">
        <h2>Norge i VM</h2>
        <NorwayHistory entries={vm.norway} />
      </section>

      <section className="section">
        <h2>Slik har formatet endret seg</h2>
        <FormatHistory periods={vm.formatHistory} />
      </section>

      <section className="section">
        <h2>Alle VM-finaler</h2>
        <h3>Flest titler</h3>
        <TitleCounts finals={vm.finalsHistory} />
        <FinalsHistory finals={vm.finalsHistory} />
      </section>

      <Sources urls={vm._meta.sources} generatedAt={vm._meta.generatedAt} />
    </>
  );
}
