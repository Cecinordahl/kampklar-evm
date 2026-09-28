// Kick-off times are shown in Norwegian time regardless of where the visitor is.
const TIME_ZONE = "Europe/Oslo";

const dateFormat = new Intl.DateTimeFormat("nb-NO", {
  weekday: "short",
  day: "numeric",
  month: "short",
  timeZone: TIME_ZONE,
});

const timeFormat = new Intl.DateTimeFormat("nb-NO", {
  hour: "2-digit",
  minute: "2-digit",
  timeZone: TIME_ZONE,
});

export function formatDate(date: Date): string {
  return dateFormat.format(date);
}

export function formatTime(date: Date): string {
  return timeFormat.format(date);
}

export function formatGoalDifference(gd: number): string {
  return gd > 0 ? `+${gd}` : String(gd);
}
