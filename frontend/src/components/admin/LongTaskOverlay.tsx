import { useEffect, useState } from "react";

/** The slow AI calls usually finish within this; the bar fills against it and stops just short. */
const EXPECTED_SECONDS = 120;

/**
 * Covers the whole page while a slow AI call runs. The call lives in this page's fetch, so
 * leaving or reloading loses the answer - the overlay says so, and the browser asks before
 * leaving. It overlays rather than replaces the page so the waiting component stays mounted.
 */
export function LongTaskOverlay({ title }: { title: string }) {
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  useEffect(() => {
    const startedAt = Date.now();
    const timer = setInterval(() => setElapsedSeconds(Math.floor((Date.now() - startedAt) / 1000)), 1000);
    const warnBeforeLeaving = (e: BeforeUnloadEvent) => e.preventDefault();
    window.addEventListener("beforeunload", warnBeforeLeaving);
    return () => {
      clearInterval(timer);
      window.removeEventListener("beforeunload", warnBeforeLeaving);
    };
  }, []);

  const progress = Math.min(elapsedSeconds / EXPECTED_SECONDS, 0.95);
  const minutes = Math.floor(elapsedSeconds / 60);
  const seconds = String(elapsedSeconds % 60).padStart(2, "0");

  return (
    <div className="long-task-overlay" role="status" aria-live="polite">
      <div className="long-task-box">
        <h2>{title}</h2>
        <p>Dette tar vanligvis 1–3 minutter.</p>
        <p className="long-task-warning">Ikke lukk fanen eller last inn siden på nytt – da stopper prosessen.</p>
        <p className="muted small">
          {minutes}:{seconds}
        </p>
        <div className="backend-status-bar" aria-hidden="true">
          <div className="backend-status-fill" style={{ width: `${progress * 100}%` }} />
        </div>
      </div>
    </div>
  );
}
