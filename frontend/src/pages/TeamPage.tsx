import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { FavoriteButton } from "../components/FavoriteButton";
import { MatchList } from "../components/MatchList";
import { EditableMatchList } from "../components/admin/EditableMatchList";
import { PlayerEditForm } from "../components/admin/PlayerEditForm";
import { TeamDetailsForm } from "../components/admin/TeamDetailsForm";
import { TeamRefreshPanel } from "../components/admin/TeamRefreshPanel";
import { useAdminMode } from "../lib/adminMode";
import { LoadError, Loading } from "../components/Status";
import { formatAge, formatIsoDate, formatStat } from "../lib/format";
import {
  useConsideredPlayers,
  useGroupMatches,
  useGroupsForTeam,
  useSquad,
  useTeam,
  useTeams,
} from "../lib/useFirestore";
import type { Player, Team } from "../types/firestore";

const POSITIONS: { code: string; label: string }[] = [
  { code: "GK", label: "Keepere" },
  { code: "DF", label: "Forsvarsspillere" },
  { code: "MF", label: "Midtbanespillere" },
  { code: "FW", label: "Angripere" },
];

export function TeamPage() {
  const { teamId } = useParams();
  const team = useTeam(teamId);
  const teams = useTeams();
  const groups = useGroupsForTeam(teamId);
  const group = groups.data[0];
  const matches = useGroupMatches(group?.id);
  const squad = useSquad(teamId);
  const considered = useConsideredPlayers(teamId);
  const admin = useAdminMode();

  if (team.loading) return <Loading />;
  if (team.error) return <LoadError error={team.error} />;
  if (!team.data || !teamId) return <p>Fant ikke laget.</p>;

  const teamMatches = matches.data.filter((m) => m.homeTeamId === teamId || m.awayTeamId === teamId);
  const { coach, tournamentHistory, refreshedAt, currentCompetition, statsAsOf, dataNotes } = team.data;

  return (
    <>
      <div className="page-title-row">
        <h1>{team.data.name}</h1>
        <FavoriteButton teamId={teamId} teamName={team.data.name} />
      </div>
      <p className="lead">
        {coach ? (
          <>
            Landslagssjef: {coach.name}
            {coach.nationality && <> ({coach.nationality})</>}
          </>
        ) : (
          "Lagdata er ikke hentet ennå."
        )}
        {group && (
          <>
            {" · "}
            <Link to={`/gruppe/${group.id}`}>Gruppe {group.name}</Link>
          </>
        )}
      </p>
      {currentCompetition && <p className="muted small">{currentCompetition}</p>}
      {admin.active && <TeamRefreshPanel team={team.data} />}

      <section className="section">
        <h2>Kamper</h2>
        {matches.error ? (
          <LoadError error={matches.error} />
        ) : admin.active ? (
          <EditableMatchList
            matches={teamMatches}
            teams={teams.data}
            highlightTeamIds={[teamId]}
            scopeLabel={team.data.name}
          />
        ) : (
          <MatchList matches={teamMatches} teams={teams.data} highlightTeamIds={[teamId]} />
        )}
      </section>

      <section className="section">
        <h2>Tropp</h2>
        {squad.loading ? (
          <Loading />
        ) : squad.error ? (
          <LoadError error={squad.error} />
        ) : squad.data.length === 0 ? (
          <p className="muted">Troppen er ikke hentet ennå.</p>
        ) : (
          <div className="squad">
            {POSITIONS.map(({ code, label }) => (
              <SquadGroup
                key={code}
                label={label}
                players={squad.data.filter((p) => p.position === code)}
                editable={admin.active}
              />
            ))}
            <SquadGroup
              label="Annet"
              players={squad.data.filter((p) => !POSITIONS.some(({ code }) => code === p.position))}
              editable={admin.active}
            />
            <SquadGroup label="Vurdert / skadet" players={considered.data} editable={admin.active} />
          </div>
        )}
        {statsAsOf && (
          <p className="muted small">
            Landskamper og mål per {formatIsoDate(statsAsOf)}, pluss kamper registrert her etterpå. – betyr ukjent.
          </p>
        )}
      </section>

      {(admin.active || (coach && (coach.bio || coach.record || coach.birthDate || coach.appointedDate))) && (
        <CoachSection team={team.data} editable={admin.active} />
      )}

      {tournamentHistory.length > 0 && (
        <section className="section">
          <h2>Turneringshistorikk</h2>
          <table className="history">
            <thead>
              <tr>
                <th scope="col">År</th>
                <th scope="col">Turnering</th>
                <th scope="col">Resultat</th>
                <th scope="col" className="wide-only">Detaljer</th>
              </tr>
            </thead>
            <tbody>
              {[...tournamentHistory]
                .sort((a, b) => b.year - a.year)
                .map((entry) => (
                  <tr key={`${entry.tournament}-${entry.year}`}>
                    <td>{entry.year}</td>
                    <td>{entry.tournament}</td>
                    <td>
                      {entry.result}
                      {entry.detail && <div className="muted small narrow-only">{entry.detail}</div>}
                    </td>
                    <td className="wide-only muted small">{entry.detail}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </section>
      )}

      {refreshedAt && (
        <p className="muted small">
          Lagdata sist oppdatert {refreshedAt.toLocaleDateString("nb-NO", { timeZone: "Europe/Oslo" })}.
          Tropp og statistikk er hentet fra offentlige kilder og kan inneholde feil.
        </p>
      )}
      {dataNotes.length > 0 && (
        <ul className="muted small data-notes">
          {dataNotes.map((note) => (
            <li key={note}>{note}</li>
          ))}
        </ul>
      )}
    </>
  );
}

function SquadGroup({ label, players, editable }: { label: string; players: Player[]; editable: boolean }) {
  const [editingId, setEditingId] = useState<string | null>(null);
  if (players.length === 0) return null;
  const columns = editable ? 6 : 5;
  return (
    <div className="squad-group">
      <h3>{label}</h3>
      <table className="squad-table">
        <thead>
          <tr>
            <th scope="col">Navn</th>
            <th scope="col" className="wide-only">Klubb</th>
            <th scope="col" className="num" title="Alder">Alder</th>
            <th scope="col" className="num" title="Landskamper">Kamper</th>
            <th scope="col" className="num" title="Landslagsmål">Mål</th>
            {editable && <th scope="col" className="visually-hidden">Rediger</th>}
          </tr>
        </thead>
        <tbody>
          {[...players]
            // Unknown caps sort last.
            .sort((a, b) => (b.caps ?? -1) - (a.caps ?? -1))
            .flatMap((p) => [
              <tr key={p.id}>
                <td>
                  {p.name}
                  {p.captain && <abbr title="Kaptein"> (K)</abbr>}
                  {p.club && <span className="muted narrow-only"> · {p.club}</span>}
                  {p.note && <div className="muted small">{p.note}</div>}
                </td>
                <td className="wide-only muted">{p.club}</td>
                <td className="num" title={p.birthYearUnverified ? "Fødselsår ikke verifisert" : undefined}>
                  {formatAge(p.birthDate, p.birthYear)}
                </td>
                <td className="num">{formatStat(p.caps)}</td>
                <td className="num">{formatStat(p.goals)}</td>
                {editable && (
                  <td className="num">
                    <button
                      type="button"
                      className="link-button"
                      aria-label={`Rediger ${p.name}`}
                      aria-expanded={editingId === p.id}
                      onClick={() => setEditingId(editingId === p.id ? null : p.id)}
                    >
                      ✏️
                    </button>
                  </td>
                )}
              </tr>,
              editingId === p.id && (
                <tr key={`${p.id}-edit`} className="edit-row">
                  <td colSpan={columns}>
                    <PlayerEditForm player={p} onDone={() => setEditingId(null)} />
                  </td>
                </tr>
              ),
            ])}
        </tbody>
      </table>
    </div>
  );
}

function CoachSection({ team, editable }: { team: Team; editable: boolean }) {
  const [editing, setEditing] = useState(false);
  const { coach } = team;
  const record = coach?.record ?? null;
  return (
    <section className="section">
      <div className="page-title-row">
        <h2>Landslagssjef</h2>
        {editable && !editing && (
          <button type="button" className="link-button small" onClick={() => setEditing(true)}>
            ✏️ Rediger landslagssjef og datanotater
          </button>
        )}
      </div>
      {editing && <TeamDetailsForm team={team} onDone={() => setEditing(false)} />}
      {!coach ? (
        <p className="muted">Ikke registrert.</p>
      ) : (
        <>
          <p>
            <strong>{coach.name}</strong>
            {coach.birthDate && <>, {formatAge(coach.birthDate, null)} år</>}
            {coach.appointedDate && <> · ansatt {formatIsoDate(coach.appointedDate)}</>}
          </p>
          {record && (
            <p className="muted small">
              {record.matches} kamper: {record.wins} seire, {record.draws} uavgjort, {record.losses} tap
              ({record.goalsFor}–{record.goalsAgainst}), per {formatIsoDate(record.asOf)}.
            </p>
          )}
          {coach.bio && <p>{coach.bio}</p>}
        </>
      )}
    </section>
  );
}
