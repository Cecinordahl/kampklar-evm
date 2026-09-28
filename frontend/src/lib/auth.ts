// Firebase Auth for the single admin account. The backend re-verifies the ID token and UID
// on every request (see AdminAuthInterceptor) - this only gates which UI the browser shows.
import { useEffect, useState } from "react";
import { onAuthStateChanged, signInWithEmailAndPassword, signOut, type User } from "firebase/auth";
import { auth } from "./firebase";

export interface AdminAuth {
  user: User | null;
  loading: boolean;
}

export function useAdminAuth(): AdminAuth {
  const [state, setState] = useState<AdminAuth>({ user: null, loading: true });

  useEffect(() => onAuthStateChanged(auth, (user) => setState({ user, loading: false })), []);

  return state;
}

export function signIn(email: string, password: string): Promise<unknown> {
  return signInWithEmailAndPassword(auth, email, password);
}

export function signOutAdmin(): Promise<void> {
  return signOut(auth);
}
