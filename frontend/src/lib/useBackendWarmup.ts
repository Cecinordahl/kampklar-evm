// Wakes the backend when the admin view opens. Render's free tier sleeps when idle and takes
// up to about a minute to start, so the admin sees a timer instead of a button that hangs.
import { useEffect, useState } from "react";

const BASE_URL = import.meta.env.VITE_BACKEND_URL;
const ATTEMPT_TIMEOUT_MS = 15_000;
const RETRY_DELAY_MS = 2_000;
const GIVE_UP_AFTER_MS = 120_000;

export type BackendStatus = "waking" | "ready" | "failed";

export function useBackendWarmup(): { status: BackendStatus; elapsedSeconds: number } {
  const [status, setStatus] = useState<BackendStatus>("waking");
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const startedAt = Date.now();
    const timer = setInterval(() => setElapsedSeconds(Math.floor((Date.now() - startedAt) / 1000)), 1000);

    async function ping() {
      while (!cancelled && Date.now() - startedAt < GIVE_UP_AFTER_MS) {
        try {
          const response = await fetch(`${BASE_URL}/health`, { signal: AbortSignal.timeout(ATTEMPT_TIMEOUT_MS) });
          if (response.ok) {
            if (!cancelled) setStatus("ready");
            return;
          }
        } catch {
          // Sleeping or still starting - retry below.
        }
        await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY_MS));
      }
      if (!cancelled) setStatus("failed");
    }

    ping().finally(() => clearInterval(timer));
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  return { status, elapsedSeconds };
}
