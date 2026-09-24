// Sole owner of the favorites localStorage key. Stored as one JSON blob so the
// storage layer can later be swapped for a per-user backend store without touching callers.

const STORAGE_KEY = "kampklar-evm:favorites";

interface FavoritesBlob {
  teamIds: string[];
  updatedAt: string;
}

function read(): FavoritesBlob {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return { teamIds: [], updatedAt: new Date(0).toISOString() };
    return JSON.parse(raw) as FavoritesBlob;
  } catch {
    return { teamIds: [], updatedAt: new Date(0).toISOString() };
  }
}

function write(teamIds: string[]): void {
  const blob: FavoritesBlob = { teamIds, updatedAt: new Date().toISOString() };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(blob));
}

export function getFavoriteTeamIds(): string[] {
  return read().teamIds;
}

export function isFavorite(teamId: string): boolean {
  return read().teamIds.includes(teamId);
}

export function toggleFavorite(teamId: string): string[] {
  const current = read().teamIds;
  const next = current.includes(teamId)
    ? current.filter((id) => id !== teamId)
    : [...current, teamId];
  write(next);
  return next;
}
