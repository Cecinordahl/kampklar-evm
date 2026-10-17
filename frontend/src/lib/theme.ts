// Night mode: follows the device setting until the visitor picks a side with the header
// toggle; the choice is kept in localStorage and mirrored on <html data-theme> (see index.html,
// which applies it before first paint).
import { useState } from "react";

export type Theme = "light" | "dark";

const STORAGE_KEY = "kampklar-theme";

function currentTheme(): Theme {
  const set = document.documentElement.dataset.theme;
  if (set === "light" || set === "dark") return set;
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export function useTheme(): { theme: Theme; toggle: () => void } {
  const [theme, setTheme] = useState<Theme>(currentTheme);

  function toggle() {
    const next: Theme = theme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = next;
    setTheme(next);
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // Private mode etc. - the switch still works for this visit.
    }
  }

  return { theme, toggle };
}
