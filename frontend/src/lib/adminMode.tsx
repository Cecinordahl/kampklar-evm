// Admin mode: when the admin is logged in and has it switched on, the regular pages show edit
// and AI buttons next to the data they change. The backend re-verifies the login on every
// write (AdminAuthInterceptor) - this only decides which UI the browser shows.
import { createContext, useContext, useState, type ReactNode } from "react";
import type { User } from "firebase/auth";
import { useAdminAuth } from "./auth";
import { useBackendWarmup, type BackendStatus } from "./useBackendWarmup";

const STORAGE_KEY = "kampklar-admin-mode";

export interface AdminMode {
  user: User | null;
  /** Firebase has not yet said whether anyone is logged in. */
  authLoading: boolean;
  /** Logged in and switched on: show the admin controls. */
  active: boolean;
  enabled: boolean;
  setEnabled: (enabled: boolean) => void;
  backend: { status: BackendStatus; elapsedSeconds: number };
  /** Active and the backend has answered - writes won't hang on a cold start. */
  ready: boolean;
}

const AdminModeContext = createContext<AdminMode | null>(null);

function readEnabled(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) !== "off";
  } catch {
    return true;
  }
}

export function AdminModeProvider({ children }: { children: ReactNode }) {
  const { user, loading: authLoading } = useAdminAuth();
  const [enabled, setEnabledState] = useState(readEnabled);
  const active = user !== null && enabled;
  const backend = useBackendWarmup(active);

  function setEnabled(value: boolean) {
    setEnabledState(value);
    try {
      localStorage.setItem(STORAGE_KEY, value ? "on" : "off");
    } catch {
      // Private mode etc. - the switch still works for this visit.
    }
  }

  return (
    <AdminModeContext.Provider
      value={{ user, authLoading, active, enabled, setEnabled, backend, ready: active && backend.status === "ready" }}
    >
      {children}
    </AdminModeContext.Provider>
  );
}

export function useAdminMode(): AdminMode {
  const mode = useContext(AdminModeContext);
  if (!mode) throw new Error("useAdminMode must be used inside AdminModeProvider");
  return mode;
}
