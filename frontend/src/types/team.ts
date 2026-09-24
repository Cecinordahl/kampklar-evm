export interface FavoriteTeam {
  id: string;
  name: string;
}

// Fixed MVP set - match data is only maintained for the groups these three teams are in.
// Adding a team later is a one-line change here, plus admin data entry for its group.
export const SUPPORTED_FAVORITE_TEAMS: FavoriteTeam[] = [
  { id: "norway", name: "Norge" },
  { id: "spain", name: "Spania" },
  { id: "france", name: "Frankrike" },
];
