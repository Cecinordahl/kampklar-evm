package no.kampklar.evm.service;

import com.anthropic.models.beta.messages.StructuredMessageCreateParams;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/** Asks Claude to look up full-time results, lineups and scorers for a group's played matches. */
@Service
public class ResultResearchService {

    private static final String SYSTEM_PROMPT = """
            You look up results of men's national team football matches for a Norwegian fan \
            site. Use web search, prefer official match reports (UEFA, the national federations) \
            and cross-check the score and scorers with a second source. Only mark a match as \
            played when it has finished. Write player names as they appear in the squad lists \
            you are given whenever it is the same player, so they can be matched.""";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);

    /** A match to look up, with each side's squad names to steer the spelling. */
    record Fixture(String matchId, String homeName, String awayName, Instant kickoff,
                   List<String> homeSquad, List<String> awaySquad) {
    }

    private final ClaudeResearchClient claude;

    public ResultResearchService(ClaudeResearchClient claude) {
        this.claude = claude;
    }

    ResultResearch research(String groupName, List<Fixture> fixtures) {
        ResultResearch research = claude.research(buildRequest(groupName, fixtures), "results in " + groupName);
        if (research.results() == null) {
            throw new ResearchException("Research for " + groupName + " returned no results");
        }
        return research;
    }

    // Package-private so a test can build it: the SDK validates the ResultResearch schema here.
    static StructuredMessageCreateParams.Builder<ResultResearch> buildRequest(String groupName, List<Fixture> fixtures) {
        String matches = fixtures.stream()
                .map(f -> "- %s: %s (home) vs %s (away), kickoff %s\n  %s squad: %s\n  %s squad: %s".formatted(
                        f.matchId(), f.homeName(), f.awayName(), DATE.format(f.kickoff()),
                        f.homeName(), String.join(", ", f.homeSquad()),
                        f.awayName(), String.join(", ", f.awaySquad())))
                .collect(Collectors.joining("\n"));
        return ClaudeResearchClient.requestBuilder(SYSTEM_PROMPT, ResultResearch.class, 15L)
                .addUserMessage("UEFA Nations League 2026/27, %s. Find the result of each of these matches:\n%s"
                        .formatted(groupName, matches));
    }
}
