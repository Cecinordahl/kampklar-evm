import type { BackendStatus } from "../../lib/useBackendWarmup";
import { Loading } from "../Status";

/** Typical cold start on Render's free tier; the bar fills against this and stops just short. */
const EXPECTED_WAKE_SECONDS = 60;
/** An awake backend answers well within this; don't flash the bar for it. */
const SHOW_AFTER_SECONDS = 2;

export function BackendStatusBanner({ status, elapsedSeconds }: { status: BackendStatus; elapsedSeconds: number }) {
  if (status === "failed") {
    return (
      <p className="error backend-status" role="alert">
        Serveren svarer ikke. Prøv å laste siden på nytt om litt.
      </p>
    );
  }

  if (elapsedSeconds < SHOW_AFTER_SECONDS) return <Loading />;

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
