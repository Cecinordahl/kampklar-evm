import { useBackendWarmup } from "../../lib/useBackendWarmup";

/** Typical cold start on Render's free tier; the bar fills against this and stops just short. */
const EXPECTED_WAKE_SECONDS = 60;
/** Don't flash the banner when the backend is already awake and answers at once. */
const SHOW_AFTER_SECONDS = 2;

export function BackendStatusBanner() {
  const { status, elapsedSeconds } = useBackendWarmup();

  if (status === "ready") return null;

  if (status === "failed") {
    return (
      <p className="error backend-status" role="alert">
        Serveren svarer ikke. Prøv å laste siden på nytt om litt.
      </p>
    );
  }

  if (elapsedSeconds < SHOW_AFTER_SECONDS) return null;

  const progress = Math.min(elapsedSeconds / EXPECTED_WAKE_SECONDS, 0.95);
  return (
    <div className="backend-status" role="status">
      <p className="small">
        Starter serveren… {elapsedSeconds} s <span className="muted">(kan ta opptil et minutt)</span>
      </p>
      <div className="backend-status-bar" aria-hidden="true">
        <div className="backend-status-fill" style={{ width: `${progress * 100}%` }} />
      </div>
    </div>
  );
}
