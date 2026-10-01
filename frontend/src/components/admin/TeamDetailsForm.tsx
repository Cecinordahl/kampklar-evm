import { useState, type FormEvent } from "react";
import { editTeamDetails } from "../../lib/adminApi";
import { useAdminMode } from "../../lib/adminMode";
import type { Team } from "../../types/firestore";

/** Manual correction of the coach's details and the team's data notes. */
export function TeamDetailsForm({ team, onDone }: { team: Team; onDone: () => void }) {
  const { ready } = useAdminMode();
  const coach = team.coach;
  const [name, setName] = useState(coach?.name ?? "");
  const [nationality, setNationality] = useState(coach?.nationality ?? "");
  const [birthDate, setBirthDate] = useState(coach?.birthDate ?? "");
  const [appointedDate, setAppointedDate] = useState(coach?.appointedDate ?? "");
  const [bio, setBio] = useState(coach?.bio ?? "");
  const [notes, setNotes] = useState(team.dataNotes.join("\n"));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await editTeamDetails(team.id, {
        coachName: name,
        coachNationality: nationality || null,
        coachBirthDate: birthDate || null,
        coachAppointedDate: appointedDate || null,
        coachBio: bio || null,
        dataNotes: notes.split("\n").map((n) => n.trim()).filter(Boolean),
      });
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ukjent feil.");
      setSaving(false);
    }
  }

  return (
    <form className="admin-form admin-inline-form" onSubmit={handleSubmit}>
      <div className="admin-form-grid">
        <label className="admin-field">
          Landslagssjef
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label className="admin-field">
          Nasjonalitet
          <input value={nationality} onChange={(e) => setNationality(e.target.value)} placeholder="f.eks. Norge" />
        </label>
        <label className="admin-field">
          Fødselsdato
          <input type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} />
        </label>
        <label className="admin-field">
          Ansatt (ÅÅÅÅ-MM-DD eller ÅÅÅÅ-MM)
          <input
            value={appointedDate}
            onChange={(e) => setAppointedDate(e.target.value)}
            pattern="\d{4}-\d{2}(-\d{2})?"
            placeholder="2020-12-07"
          />
        </label>
      </div>
      <label className="admin-field">
        Om landslagssjefen
        <textarea rows={4} value={bio} onChange={(e) => setBio(e.target.value)} />
      </label>
      <label className="admin-field">
        Datanotater (ett per linje – vises nederst på lagsiden)
        <textarea rows={3} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </label>
      {coach?.record && (
        <p className="muted small">Kamprekorden ({coach.record.matches} kamper) beholdes som den er.</p>
      )}
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      <div className="admin-score-row">
        <button type="submit" className="button button-small" disabled={!ready || saving}>
          {saving ? "Lagrer…" : "Lagre"}
        </button>
        <button type="button" className="link-button" onClick={onDone}>
          Avbryt
        </button>
      </div>
    </form>
  );
}
