import { useState, type FormEvent } from "react";
import { editPlayer } from "../../lib/adminApi";
import { useAdminMode } from "../../lib/adminMode";
import type { Player } from "../../types/firestore";

/** "" in a number field means unknown (null), never 0. */
function toNumber(value: string): number | null {
  return value.trim() === "" ? null : Number(value);
}

/** Manual correction of one player, opened inline under their row in the squad table. */
export function PlayerEditForm({ player, onDone }: { player: Player; onDone: () => void }) {
  const { ready } = useAdminMode();
  const [club, setClub] = useState(player.club ?? "");
  const [position, setPosition] = useState(player.position);
  const [birthDate, setBirthDate] = useState(player.birthDate ?? "");
  const [birthYear, setBirthYear] = useState(player.birthYear?.toString() ?? "");
  // Players stored before squadStatus existed only have inSquad.
  const [squadStatus, setSquadStatus] = useState(player.squadStatus ?? (player.inSquad ? "confirmed" : ""));
  const [captain, setCaptain] = useState(player.captain);
  const [note, setNote] = useState(player.note ?? "");
  const [caps, setCaps] = useState(player.caps?.toString() ?? "");
  const [goals, setGoals] = useState(player.goals?.toString() ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const statsChanged = toNumber(caps) !== player.caps || toNumber(goals) !== player.goals;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await editPlayer(player.id, {
        club: club || null,
        position,
        birthDate: birthDate || null,
        birthYear: toNumber(birthYear),
        squadStatus: squadStatus === "" ? null : (squadStatus as "confirmed" | "considered"),
        captain,
        note: note || null,
        caps: toNumber(caps),
        goals: toNumber(goals),
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
          Klubb
          <input value={club} onChange={(e) => setClub(e.target.value)} />
        </label>
        <label className="admin-field">
          Posisjon
          <select value={position} onChange={(e) => setPosition(e.target.value)}>
            <option value="GK">Keeper</option>
            <option value="DF">Forsvar</option>
            <option value="MF">Midtbane</option>
            <option value="FW">Angrep</option>
          </select>
        </label>
        <label className="admin-field">
          Tropp
          <select value={squadStatus} onChange={(e) => setSquadStatus(e.target.value as typeof squadStatus)}>
            <option value="confirmed">I troppen</option>
            <option value="considered">Vurdert / skadet</option>
            <option value="">Ikke i troppen</option>
          </select>
        </label>
        <label className="admin-field">
          Fødselsdato
          <input type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} />
        </label>
        {!birthDate && (
          <label className="admin-field">
            Fødselsår (hvis dato er ukjent)
            <input type="number" min={1900} value={birthYear} onChange={(e) => setBirthYear(e.target.value)} />
          </label>
        )}
        <label className="admin-field admin-checkbox">
          <input type="checkbox" checked={captain} onChange={(e) => setCaptain(e.target.checked)} />
          Kaptein
        </label>
      </div>
      <label className="admin-field">
        Notat
        <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Vises under navnet" />
      </label>

      <fieldset className="admin-baseline">
        <legend>Landskamper og mål (totalt)</legend>
        <div className="admin-score-row">
          <label className="admin-field admin-field-narrow">
            Kamper
            <input type="number" min={0} value={caps} onChange={(e) => setCaps(e.target.value)} placeholder="–" />
          </label>
          <label className="admin-field admin-field-narrow">
            Mål
            <input type="number" min={0} value={goals} onChange={(e) => setGoals(e.target.value)} placeholder="–" />
          </label>
        </div>
        <p className="small muted">Tomt felt = ukjent («–»), ikke 0.</p>
        {statsChanged && (
          <p className="notice small" role="alert">
            ⚠️ Baseline-korrigering: skriv inn <strong>totalen inkludert kamper registrert her</strong>. Kamper du lagrer
            senere legges fortsatt til automatisk – ikke trekk dem fra eller legg dem til selv, ellers telles de dobbelt.
          </p>
        )}
      </fieldset>

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
