// Mirrors the Firestore schema written by the backend (see backend model/ and
// service/FirestoreDocuments.java). Field names here must match that mapping exactly.
import type { DocumentData, DocumentSnapshot, QueryDocumentSnapshot } from "firebase/firestore";

export interface Standing {
  teamId: string;
  rank: number;
  played: number;
  won: number;
  drawn: number;
  lost: number;
  goalsFor: number;
  goalsAgainst: number;
  goalDifference: number;
  points: number;
}

export interface Group {
  id: string;
  competitionId: string;
  divisionId: string;
  name: string;
  teamIds: string[];
  standings: Standing[];
}

export type MatchStatus = "SCHEDULED" | "FINISHED";

export interface Match {
  id: string;
  competitionId: string;
  groupId: string;
  homeTeamId: string;
  awayTeamId: string;
  kickoff: Date;
  status: MatchStatus;
  homeGoals: number | null;
  awayGoals: number | null;
  homeLineup: string[];
  awayLineup: string[];
  homeScorerIds: string[];
  awayScorerIds: string[];
}

// null everywhere below means "unknown" - shown as "–", never as 0.
export interface CoachRecord {
  matches: number;
  wins: number;
  draws: number;
  losses: number;
  goalsFor: number;
  goalsAgainst: number;
  asOf: string;
}

export interface Coach {
  name: string;
  nationality: string | null;
  birthDate: string | null;
  // ISO, sometimes month precision only ("2022-12").
  appointedDate: string | null;
  bio: string | null;
  record: CoachRecord | null;
}

export interface TournamentHistoryEntry {
  tournament: string;
  year: number;
  result: string;
  detail: string | null;
}

export interface Team {
  id: string;
  name: string;
  currentCompetition: string | null;
  coach: Coach | null;
  tournamentHistory: TournamentHistoryEntry[];
  // When the caps/goals baseline was published (ISO date).
  statsAsOf: string | null;
  dataNotes: string[];
  refreshedAt: Date | null;
}

export type Position = "GK" | "DF" | "MF" | "FW";

export interface Player {
  id: string;
  teamId: string;
  name: string;
  position: Position | string;
  club: string | null;
  // Age is computed from these, never stored.
  birthDate: string | null;
  birthYear: number | null;
  birthYearUnverified: boolean;
  caps: number | null;
  goals: number | null;
  inSquad: boolean;
  squadStatus: "confirmed" | "considered" | null;
  captain: boolean;
  note: string | null;
}

type Snapshot = QueryDocumentSnapshot<DocumentData> | DocumentSnapshot<DocumentData>;

export function toGroup(doc: Snapshot): Group {
  const d = doc.data() ?? {};
  return {
    id: doc.id,
    competitionId: d.competitionId,
    divisionId: d.divisionId,
    name: d.name,
    teamIds: d.teamIds ?? [],
    standings: d.standings ?? [],
  };
}

export function toMatch(doc: Snapshot): Match {
  const d = doc.data() ?? {};
  return {
    id: doc.id,
    competitionId: d.competitionId,
    groupId: d.groupId,
    homeTeamId: d.homeTeamId,
    awayTeamId: d.awayTeamId,
    kickoff: d.kickoff.toDate(),
    status: d.status,
    homeGoals: d.homeGoals ?? null,
    awayGoals: d.awayGoals ?? null,
    homeLineup: d.homeLineup ?? [],
    awayLineup: d.awayLineup ?? [],
    homeScorerIds: d.homeScorerIds ?? [],
    awayScorerIds: d.awayScorerIds ?? [],
  };
}

export function toTeam(doc: Snapshot): Team {
  const d = doc.data() ?? {};
  return {
    id: doc.id,
    name: d.name ?? doc.id,
    currentCompetition: d.currentCompetition ?? null,
    coach: d.coach
      ? {
          name: d.coach.name,
          nationality: d.coach.nationality ?? null,
          birthDate: d.coach.birthDate ?? null,
          appointedDate: d.coach.appointedDate ?? null,
          bio: d.coach.bio ?? null,
          record: d.coach.record ?? null,
        }
      : null,
    tournamentHistory: (d.tournamentHistory ?? []).map((e: TournamentHistoryEntry) => ({ ...e, detail: e.detail ?? null })),
    statsAsOf: d.statsAsOf ?? null,
    dataNotes: d.dataNotes ?? [],
    refreshedAt: d.refreshedAt ? d.refreshedAt.toDate() : null,
  };
}

export function toPlayer(doc: Snapshot): Player {
  const d = doc.data() ?? {};
  return {
    id: doc.id,
    teamId: d.teamId,
    name: d.name,
    position: d.position,
    club: d.club ?? null,
    birthDate: d.birthDate ?? null,
    birthYear: d.birthYear ?? null,
    birthYearUnverified: d.birthYearUnverified === true,
    caps: d.caps ?? null,
    goals: d.goals ?? null,
    inSquad: d.inSquad !== false,
    squadStatus: d.squadStatus ?? null,
    captain: d.captain === true,
    note: d.note ?? null,
  };
}
