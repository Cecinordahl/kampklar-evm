// Circular flag images for the teams in the competition, keyed by team id. Only these SVGs
// are bundled (flag-icons, MIT); an unknown team gets the initials fallback in <Flag>.
import be from "flag-icons/flags/1x1/be.svg";
import cz from "flag-icons/flags/1x1/cz.svg";
import dk from "flag-icons/flags/1x1/dk.svg";
import es from "flag-icons/flags/1x1/es.svg";
import fr from "flag-icons/flags/1x1/fr.svg";
import eng from "flag-icons/flags/1x1/gb-eng.svg";
import wls from "flag-icons/flags/1x1/gb-wls.svg";
import hr from "flag-icons/flags/1x1/hr.svg";
import it from "flag-icons/flags/1x1/it.svg";
import no from "flag-icons/flags/1x1/no.svg";
import pt from "flag-icons/flags/1x1/pt.svg";
import tr from "flag-icons/flags/1x1/tr.svg";

const FLAGS: Record<string, string> = {
  belgium: be,
  czechia: cz,
  denmark: dk,
  spain: es,
  france: fr,
  england: eng,
  wales: wls,
  croatia: hr,
  italy: it,
  norway: no,
  portugal: pt,
  turkiye: tr,
};

export function flagFor(teamId: string): string | undefined {
  return FLAGS[teamId];
}
