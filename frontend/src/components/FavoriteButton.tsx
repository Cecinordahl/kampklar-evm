import { useFavorites } from "../lib/useFavorites";

export function FavoriteButton({ teamId, teamName }: { teamId: string; teamName: string }) {
  const { favoriteIds, toggle } = useFavorites();
  const isFavorite = favoriteIds.includes(teamId);

  return (
    <button
      type="button"
      className={`favorite-button${isFavorite ? " is-favorite" : ""}`}
      aria-pressed={isFavorite}
      aria-label={isFavorite ? `Slutt å følge ${teamName}` : `Følg ${teamName}`}
      onClick={() => toggle(teamId)}
    >
      <span aria-hidden="true">{isFavorite ? "★" : "☆"}</span>
      {isFavorite ? "Følger" : "Følg"}
    </button>
  );
}
