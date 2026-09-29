package no.kampklar.evm.service;

import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.StructuredMessageCreateParams;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Asks Claude to research a national team's current squad, coach and tournament history
 * using web search.
 */
@Service
public class TeamResearchService {

    private static final String SYSTEM_PROMPT = """
            You research men's national football teams for a Norwegian fan site. Use web search \
            to find current facts, prefer the national federation's own site and other primary \
            sources, and cross-check caps and goals, since secondary sites often lag. Report what \
            the sources say; if a figure cannot be verified, give your best-supported value \
            rather than omitting the player. Write coach nationality and tournament results in \
            Norwegian bokmål (e.g. "Norge", "Gruppespill", "Kvartfinale", "Vinner", \
            "Ikke kvalifisert").""";

    private final ClaudeResearchClient claude;

    public TeamResearchService(ClaudeResearchClient claude) {
        this.claude = claude;
    }

    public TeamResearch research(String teamId, String teamName) {
        TeamResearch research = claude.research(buildRequest(teamId, teamName), teamId);
        if (research.coach() == null || research.squad() == null || research.squad().isEmpty()) {
            throw new ResearchException("Research for " + teamId + " returned no coach or squad");
        }
        return research;
    }

    // Package-private so a test can build it: the SDK validates the TeamResearch schema here.
    static StructuredMessageCreateParams.Builder<TeamResearch> buildRequest(String teamId, String teamName) {
        // Any site: squad announcements live on each country's own federation site.
        return ClaudeResearchClient.requestBuilder(ClaudeResearchClient.MODEL, BetaOutputConfig.Effort.MEDIUM,
                        SYSTEM_PROMPT, TeamResearch.class, 6L, List.of())
                .addUserMessage("Research the men's senior national football team of %s (team id \"%s\")."
                        .formatted(teamName, teamId));
    }
}
