import type { Match } from "../types/firestore";

// Results usually land within two hours of kickoff; only after that is a game flagged as missing one.
const RESULT_GRACE_MS = 2 * 60 * 60 * 1000;

/** Kicked off more than two hours ago and still has no saved result. */
export function isResultMissing(match: Match, now = new Date()): boolean {
  return match.status !== "FINISHED" && now.getTime() - match.kickoff.getTime() > RESULT_GRACE_MS;
}
