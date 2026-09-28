// Live Firestore subscriptions: a result entered in the admin view updates every open
// public page within a second or so, without a reload.
import { useEffect, useState } from "react";
import {
  collection,
  doc,
  onSnapshot,
  query,
  where,
  type DocumentData,
  type DocumentSnapshot,
  type Query,
  type QueryDocumentSnapshot,
} from "firebase/firestore";
import { db } from "./firebase";
import {
  toGroup,
  toMatch,
  toPlayer,
  toTeam,
  type Group,
  type Match,
  type Player,
  type Team,
} from "../types/firestore";

export interface Live<T> {
  data: T;
  loading: boolean;
  error: Error | null;
}

function useLiveQuery<T>(
  key: string | null,
  buildQuery: () => Query<DocumentData>,
  convert: (doc: QueryDocumentSnapshot<DocumentData>) => T,
): Live<T[]> {
  const [state, setState] = useState<Live<T[]>>({ data: [], loading: key !== null, error: null });

  useEffect(() => {
    if (key === null) {
      setState({ data: [], loading: false, error: null });
      return;
    }
    setState((s) => ({ ...s, loading: true }));
    return onSnapshot(
      buildQuery(),
      (snap) => setState({ data: snap.docs.map(convert), loading: false, error: null }),
      (error) => setState({ data: [], loading: false, error }),
    );
    // `key` identifies the query; buildQuery/convert are recreated every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return state;
}

function useLiveDoc<T>(
  path: [string, string] | null,
  convert: (doc: DocumentSnapshot<DocumentData>) => T,
): Live<T | null> {
  const key = path ? path.join("/") : null;
  const [state, setState] = useState<Live<T | null>>({ data: null, loading: key !== null, error: null });

  useEffect(() => {
    if (!path) {
      setState({ data: null, loading: false, error: null });
      return;
    }
    setState((s) => ({ ...s, loading: true }));
    return onSnapshot(
      doc(db, path[0], path[1]),
      (snap) => setState({ data: snap.exists() ? convert(snap) : null, loading: false, error: null }),
      (error) => setState({ data: null, loading: false, error }),
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return state;
}

/** Every team, keyed by id - a handful of small documents, used to turn ids into names. */
export function useTeams(): Live<Map<string, Team>> {
  const live = useLiveQuery("teams", () => query(collection(db, "teams")), toTeam);
  return { ...live, data: new Map(live.data.map((t) => [t.id, t])) };
}

export function useTeam(teamId: string | undefined): Live<Team | null> {
  return useLiveDoc(teamId ? ["teams", teamId] : null, toTeam);
}

export function useGroup(groupId: string | undefined): Live<Group | null> {
  return useLiveDoc(groupId ? ["groups", groupId] : null, toGroup);
}

/** Every group - used by the admin view's group picker. */
export function useGroups(): Live<Group[]> {
  const live = useLiveQuery("groups", () => query(collection(db, "groups")), toGroup);
  return { ...live, data: [...live.data].sort((a, b) => a.name.localeCompare(b.name)) };
}

/** The group(s) a team plays in. One per competition; this MVP tracks a single competition. */
export function useGroupsForTeam(teamId: string | undefined): Live<Group[]> {
  return useLiveQuery(
    teamId ? `groups-for-${teamId}` : null,
    () => query(collection(db, "groups"), where("teamIds", "array-contains", teamId)),
    toGroup,
  );
}

export function useGroupMatches(groupId: string | undefined): Live<Match[]> {
  const live = useLiveQuery(
    groupId ? `matches-${groupId}` : null,
    () => query(collection(db, "matches"), where("groupId", "==", groupId)),
    toMatch,
  );
  // Sorted client-side: ordering in the query would need a composite index.
  return { ...live, data: [...live.data].sort((a, b) => a.kickoff.getTime() - b.kickoff.getTime()) };
}

export function useSquad(teamId: string | undefined): Live<Player[]> {
  return useLiveQuery(
    teamId ? `squad-${teamId}` : null,
    () => query(collection(db, "players"), where("teamId", "==", teamId), where("inSquad", "==", true)),
    toPlayer,
  );
}
