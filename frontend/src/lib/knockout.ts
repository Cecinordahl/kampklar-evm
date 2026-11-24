// The Nations League knockout stage (quarter-finals in March, finals in June 2027). It lives in
// content/nations-league.json, not Firestore: the backend only models group matches.
import nl from "../content/nations-league.json";

const ko = nl.knockout;

export interface KnockoutLeg {
  tieId: string;
  leg: 1 | 2;
  homeTeamId: string;
  awayTeamId: string;
  kickoff: Date;
  venue?: string;
}

export interface QuarterFinal {
  id: string;
  home: string;
  away: string;
  legs: KnockoutLeg[];
}

export const quarterFinals: QuarterFinal[] = ko.quarterFinals.map((qf) => ({
  id: qf.id,
  home: qf.home,
  away: qf.away,
  // The first-named team is at home in the first leg; the return leg swaps them.
  legs: qf.legs.map((leg, i) => ({
    tieId: qf.id,
    leg: (i + 1) as 1 | 2,
    homeTeamId: i === 0 ? qf.home : qf.away,
    awayTeamId: i === 0 ? qf.away : qf.home,
    kickoff: new Date(leg.kickoff),
    venue: "venue" in leg ? leg.venue : undefined,
  })),
}));

export function knockoutTeamName(teamId: string): string {
  return (ko.teamNames as Record<string, string>)[teamId] ?? teamId;
}

export function tieLabel(tieId: string): string {
  const qf = quarterFinals.find((q) => q.id === tieId);
  return qf ? `${knockoutTeamName(qf.home)}/${knockoutTeamName(qf.away)}` : tieId;
}

export function quarterFinalFor(teamId: string): QuarterFinal | undefined {
  return quarterFinals.find((qf) => qf.home === teamId || qf.away === teamId);
}

/** The team's next knockout leg that hasn't kicked off yet. */
export function nextKnockoutLeg(teamId: string, now = new Date()): KnockoutLeg | undefined {
  return quarterFinalFor(teamId)?.legs.find((leg) => leg.kickoff > now);
}
