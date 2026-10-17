import { flagFor } from "../lib/flags";

/** A round team flag; decorative, since the team name always sits next to it. */
export function Flag({ teamId, size = "sm" }: { teamId: string; size?: "sm" | "lg" }) {
  const src = flagFor(teamId);
  if (!src) {
    return (
      <span className={`flag flag-${size} flag-fallback`} aria-hidden="true">
        {teamId.slice(0, 2).toUpperCase()}
      </span>
    );
  }
  return <img className={`flag flag-${size}`} src={src} alt="" aria-hidden="true" loading="lazy" />;
}
