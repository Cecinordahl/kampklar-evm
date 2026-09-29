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

/** Unknown (null) is shown as a dash, never as 0. */
export function formatStat(value: number | null): string {
  return value === null ? "–" : String(value);
}

/** Age today in Norway; from the exact birth date, or approximate ("ca. 21") from the year alone. */
export function formatAge(birthDate: string | null, birthYear: number | null, today = new Date()): string {
  const [year, month, day] = new Intl.DateTimeFormat("en-CA", { timeZone: TIME_ZONE })
    .format(today)
    .split("-")
    .map(Number);
  if (birthDate) {
    const [by, bm, bd] = birthDate.split("-").map(Number);
    const hadBirthday = month > bm || (month === bm && day >= bd);
    return String(year - by - (hadBirthday ? 0 : 1));
  }
  if (birthYear !== null) return `ca. ${year - birthYear}`;
  return "–";
}

const isoDateFormat = new Intl.DateTimeFormat("nb-NO", { day: "numeric", month: "long", year: "numeric", timeZone: "UTC" });
const isoMonthFormat = new Intl.DateTimeFormat("nb-NO", { month: "long", year: "numeric", timeZone: "UTC" });

/** "2022-12" -> "desember 2022", "2020-12-07" -> "7. desember 2020". */
export function formatIsoDate(iso: string): string {
  const date = new Date(iso.length === 7 ? `${iso}-01` : iso);
  return (iso.length === 7 ? isoMonthFormat : isoDateFormat).format(date);
}
