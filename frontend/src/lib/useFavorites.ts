import { useCallback, useEffect, useState } from "react";
import { getFavoriteTeamIds, toggleFavorite } from "./favorites";

const CHANGE_EVENT = "kampklar-evm:favorites-changed";

/** Favorites as React state, kept in sync across every component (and tab) that uses it. */
export function useFavorites(): { favoriteIds: string[]; toggle: (teamId: string) => void } {
  const [favoriteIds, setFavoriteIds] = useState<string[]>(getFavoriteTeamIds);

  useEffect(() => {
    const sync = () => setFavoriteIds(getFavoriteTeamIds());
    window.addEventListener(CHANGE_EVENT, sync);
    window.addEventListener("storage", sync); // another tab changed them
    return () => {
      window.removeEventListener(CHANGE_EVENT, sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  const toggle = useCallback((teamId: string) => {
    toggleFavorite(teamId);
    window.dispatchEvent(new Event(CHANGE_EVENT));
  }, []);

  return { favoriteIds, toggle };
}
