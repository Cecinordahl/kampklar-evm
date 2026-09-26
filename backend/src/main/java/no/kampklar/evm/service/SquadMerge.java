package no.kampklar.evm.service;

import no.kampklar.evm.model.Player;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides how a freshly researched squad changes a team's stored players. Players are matched
 * by normalized name, since research returns names, not our ids.
 *
 * <p>Caps and goals are only taken from research for players seen for the first time. After
 * that, match entry owns them (see {@link PlayerStatChanges}) - letting research overwrite
 * them could silently undo a result entered before the web sources caught up.
 */
final class SquadMerge {

    /** Existing players carry only the fields that changed, so writes can be partial updates. */
    record Plan(List<Player> added, List<Player> updated, List<Player> leftSquad) {
    }

    private SquadMerge() {
    }

    static Plan plan(String teamId, List<Player> existing, List<TeamResearch.Player> squad) {
        Map<String, Player> existingByName = new HashMap<>();
        Set<String> takenIds = new HashSet<>();
        for (Player player : existing) {
            existingByName.put(normalize(player.name()), player);
            takenIds.add(player.id());
        }

        List<Player> added = new ArrayList<>();
        List<Player> updated = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        for (TeamResearch.Player researched : squad) {
            String key = normalize(researched.name());
            if (key.isEmpty() || !seenNames.add(key)) {
                continue; // research occasionally lists a player twice
            }
            Player current = existingByName.get(key);
            if (current == null) {
                String id = uniqueId(teamId + "-" + key, takenIds);
                added.add(new Player(id, teamId, researched.name(), researched.position(), researched.club(),
                        researched.caps(), researched.goals(), true));
            } else {
                updated.add(new Player(current.id(), teamId, researched.name(), researched.position(),
                        researched.club(), current.caps(), current.goals(), true));
            }
        }

        List<Player> leftSquad = existing.stream()
                .filter(Player::inSquad)
                .filter(p -> !seenNames.contains(normalize(p.name())))
                .map(p -> new Player(p.id(), p.teamId(), p.name(), p.position(), p.club(), p.caps(), p.goals(), false))
                .toList();

        return new Plan(added, updated, leftSquad);
    }

    /** "Martin Ødegaard" and "Martin Odegaard" both become "martin-odegaard". */
    static String normalize(String name) {
        String ascii = Normalizer.normalize(name.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                // Letters that are not composed characters, so NFD leaves them alone.
                .replace("ø", "o").replace("æ", "ae").replace("ß", "ss")
                .replace("đ", "d").replace("ł", "l").replace("ı", "i");
        return ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static String uniqueId(String base, Set<String> takenIds) {
        String id = base;
        for (int n = 2; takenIds.contains(id); n++) {
            id = base + "-" + n;
        }
        takenIds.add(id);
        return id;
    }
}
