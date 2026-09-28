// Calls to the backend's /admin/** endpoints, authenticated with the signed-in admin's
// Firebase ID token. Public pages never import this - they read Firestore directly.
import { auth } from "./firebase";
import type { MatchStatus, Standing } from "../types/firestore";

const BASE_URL = import.meta.env.VITE_BACKEND_URL;

export class AdminApiError extends Error {}

async function adminFetch<T>(path: string, options: RequestInit): Promise<T> {
  const user = auth.currentUser;
  if (!user) {
    throw new AdminApiError("Ikke innlogget.");
  }
  const token = await user.getIdToken();
  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: { ...options.headers, Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
  });
  if (!response.ok) {
    const body: { message?: string } | null = await response.json().catch(() => null);
    throw new AdminApiError(body?.message ?? `Forespørselen feilet (${response.status}).`);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export interface MatchSaveRequest {
  competitionId: string;
  groupId: string;
  homeTeamId: string;
  awayTeamId: string;
  kickoff: string; // ISO instant
  status: MatchStatus;
  homeGoals: number | null;
  awayGoals: number | null;
  homeLineup: string[];
  awayLineup: string[];
  homeScorerIds: string[];
  awayScorerIds: string[];
}

export interface MatchSavedResponse {
  matchId: string;
  groupId: string;
  standings: Standing[];
}

export function saveMatch(matchId: string, body: MatchSaveRequest): Promise<MatchSavedResponse> {
  return adminFetch(`/admin/matches/${matchId}`, { method: "PUT", body: JSON.stringify(body) });
}

export interface TeamRefreshResult {
  teamId: string;
  coach: { name: string; nationality: string } | null;
  tournamentsRecorded: number;
  playersAdded: string[];
  playersUpdated: number;
  playersLeftSquad: string[];
}

export function refreshTeam(teamId: string): Promise<TeamRefreshResult> {
  return adminFetch(`/admin/teams/${teamId}/refresh`, { method: "POST" });
}
