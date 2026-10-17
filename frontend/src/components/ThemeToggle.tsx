import { useTheme } from "../lib/theme";

/** Header button that switches between day and night mode. */
export function ThemeToggle() {
  const { theme, toggle } = useTheme();
  const night = theme === "dark";
  return (
    <button
      type="button"
      className="theme-toggle"
      onClick={toggle}
      aria-pressed={night}
      aria-label="Nattmodus"
      title={night ? "Bytt til dagmodus" : "Bytt til nattmodus"}
    >
      <span aria-hidden="true">{night ? "☀️" : "🌙"}</span>
    </button>
  );
}
