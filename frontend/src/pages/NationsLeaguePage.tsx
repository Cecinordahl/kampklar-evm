import { Link } from "react-router-dom";
import nl from "../content/nations-league.json";
import { Flow, KeyFacts, Sources, type FlowTone } from "../components/tournament/TournamentParts";

export function NationsLeaguePage() {
  const { current, euro2028, worldCup } = nl;
  const a = current.levelA;

  return (
    <>
      <p className="eyebrow">UEFA</p>
      <h1>Nations League</h1>

      <section className="section">
        <h2>Hva er Nations League?</h2>
        {nl.intro.map((paragraph) => (
          <p key={paragraph}>{paragraph}</p>
        ))}
        <table className="history">
          <thead>
            <tr>
              <th scope="col">Nivå</th>
              <th scope="col">Som i et seriesystem</th>
            </tr>
          </thead>
          <tbody>
            {nl.levelsTable.map((row) => (
              <tr key={row.level}>
                <td>
                  <strong>{row.level}</strong>
                </td>
                <td>{row.likeLeague}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="section">
        <h2>Oppsettet i {current.name}</h2>
        <KeyFacts
          facts={[
            { label: "Nivåer", value: current.levels.join(", ") },
            { label: "Lag på nivå A", value: a.teams },
            { label: "Grupper på nivå A", value: `${a.groups} à ${a.teamsPerGroup}` },
            { label: "Kamper per lag", value: a.matchesPerTeam },
          ]}
        />
        <p>Hvert nivå er delt i grupper. På nivå A gjelder: {a.format.toLowerCase()}</p>
        <p>
          {current.norway.text}{" "}
          <Link to={`/gruppe/${current.norway.groupId}`}>Se tabellen og kampene i Norges gruppe</Link>.
        </p>
        {/* TODO: Ingen fast gruppetabell her - den ville blitt utdatert. Lenken over går til gruppesiden,
            som viser tabellen fra registrerte resultater. */}

        <h3>Hva plasseringen i gruppa betyr på nivå A</h3>
        <Flow
          caption="Plasseringen i gruppa på nivå A avgjør om laget går videre, må spille for å bli, eller rykker ned."
          stages={[
            [{ label: "Gruppespill på nivå A", sub: `${a.groups} grupper à ${a.teamsPerGroup} · ${a.matchesPerTeam} kamper` }],
            current.outcomesA.map((o) => ({ label: o.place, sub: o.result, tone: o.tone as FlowTone })),
          ]}
        />
      </section>

      <section className="section">
        <h2>Rangeringen</h2>
        <ul className="fact-list">
          {nl.ranking.map((point) => (
            <li key={point}>{point}</li>
          ))}
        </ul>
      </section>

      <section className="section">
        <h2>Slik brukes rangeringen: EM 2028</h2>
        <h3>Seeding</h3>
        <p>{euro2028.seeding}</p>
        <h3>Reservebillett</h3>
        <p>{euro2028.backup}</p>
        <p>{euro2028.backupSlots}</p>
        <h3>Fra plassering til EM-sjanse</h3>
        <table className="history">
          <thead>
            <tr>
              <th scope="col">Plassering i Nations League</th>
              <th scope="col">Seeding i EM-kvaliken</th>
              <th scope="col">Reservebillett (playoff)</th>
            </tr>
          </thead>
          <tbody>
            {euro2028.table.map((row) => (
              <tr key={row.position}>
                <td>{row.position}</td>
                <td>{row.seeding}</td>
                <td>{row.backup}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <p>{euro2028.qualifying}</p>
        <p>
          <Link to="/em">Les mer om EM 2028 og EM-kvaliken →</Link>
        </p>
      </section>

      <section className="section">
        <h2>Slik brukes rangeringen: VM</h2>
        <p>{worldCup.wc2026}</p>
        <h3>VM 2030</h3>
        <p className="notice">{worldCup.wc2030Status}</p>
        <p>{worldCup.wc2030Intro}</p>
        <p>{worldCup.wc2030Leagues}</p>
        <p>{worldCup.wc2030Direct}</p>
        <p>
          <Link to="/vm">Les mer om VM 2030 og den nye kvaliken →</Link>
        </p>
      </section>

      <Sources urls={nl._meta.sources} generatedAt={nl._meta.generatedAt} />
    </>
  );
}
