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

export interface Coach {
  name: string;
  nationality: string;
}

export interface TournamentHistoryEntry {
  tournament: string;
  year: number;
  result: string;
}

export interface Team {
  id: string;
  name: string;
  coach: Coach | null;
  tournamentHistory: TournamentHistoryEntry[];
  refreshedAt: Date | null;
}

export type Position = "GK" | "DF" | "MF" | "FW";

export interface Player {
  id: string;
  teamId: string;
  name: string;
  position: Position | string;
  club: string | null;
  caps: number;
  goals: number;
  inSquad: boolean;
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
    coach: d.coach ?? null,
    tournamentHistory: d.tournamentHistory ?? [],
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
    caps: d.caps ?? 0,
    goals: d.goals ?? 0,
    inSquad: d.inSquad !== false,
  };
}
