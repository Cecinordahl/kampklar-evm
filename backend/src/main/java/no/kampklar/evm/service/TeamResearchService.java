package no.kampklar.evm.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaThinkingConfigAdaptive;
import com.anthropic.models.beta.messages.BetaWebSearchTool20260209;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.anthropic.models.beta.messages.StructuredContentBlock;
import com.anthropic.models.beta.messages.StructuredMessage;
import com.anthropic.models.beta.messages.StructuredMessageCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Asks Claude to research a national team's current squad, coach and tournament history
 * using web search, and returns it as typed data (structured output - no free-text parsing).
 *
 * <p>This is the only paid call in the stack; each refresh costs roughly $0.30-0.60, logged
 * per call via the usage line below.
 */
@Service
public class TeamResearchService {

    private static final Logger log = LoggerFactory.getLogger(TeamResearchService.class);

    private static final String MODEL = "claude-opus-5";
    // Web search runs a server-side loop that may hand back a paused turn to be resumed.
    private static final int MAX_CONTINUATIONS = 5;

    private static final String SYSTEM_PROMPT = """
            You research men's national football teams for a Norwegian fan site. Use web search \
            to find current facts, prefer the national federation's own site and other primary \
            sources, and cross-check caps and goals, since secondary sites often lag. Report what \
            the sources say; if a figure cannot be verified, give your best-supported value \
            rather than omitting the player. Write coach nationality and tournament results in \
            Norwegian bokmål (e.g. "Norge", "Gruppespill", "Kvartfinale", "Vinner", \
            "Ikke kvalifisert").""";

    private final AnthropicClient client;

    public TeamResearchService(@Value("${anthropic.api-key}") String apiKey) {
        // No key configured (e.g. `mvn test`): the service exists but refuses to research.
        this.client = apiKey == null || apiKey.isBlank() ? null : AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofMinutes(10)) // several searches plus thinking can take minutes
                .build();
    }

    public TeamResearch research(String teamId, String teamName) {
        if (client == null) {
            throw new ResearchUnavailableException("ANTHROPIC_API_KEY is not configured");
        }

        StructuredMessageCreateParams.Builder<TeamResearch> request = buildRequest(teamId, teamName);

        try {
            for (int attempt = 0; attempt <= MAX_CONTINUATIONS; attempt++) {
                StructuredMessage<TeamResearch> response = client.beta().messages().create(request.build());
                log.info("Team research for {} (call {}): usage {}", teamId, attempt + 1, response.usage());

                BetaStopReason stopReason = response.stopReason().orElse(null);
                if (BetaStopReason.PAUSE_TURN.equals(stopReason)) {
                    // Resend with the paused assistant turn appended; the server resumes the search loop.
                    request.addMessage(response.rawMessage());
                    continue;
                }
                if (BetaStopReason.REFUSAL.equals(stopReason)) {
                    throw new TeamResearchException("Claude declined to research " + teamId
                            + response.rawMessage().stopDetails().map(d -> " (" + d.category() + ")").orElse(""));
                }
                if (BetaStopReason.MAX_TOKENS.equals(stopReason)) {
                    throw new TeamResearchException("Research answer for " + teamId + " was cut off");
                }
                return finalAnswer(response, teamId);
            }
        } catch (AnthropicException e) {
            throw new TeamResearchException("Claude API call failed for " + teamId + ": " + e.getMessage(), e);
        }
        throw new TeamResearchException("Research for " + teamId + " did not finish after "
                + MAX_CONTINUATIONS + " continuations");
    }

    // Package-private so a test can build it: the SDK validates the TeamResearch schema here.
    static StructuredMessageCreateParams.Builder<TeamResearch> buildRequest(String teamId, String teamName) {
        return MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(16000L)
                // Re-runs a (rare) safety-classifier refusal on Anthropic's recommended fallback model.
                .addBeta("server-side-fallback-2026-07-01")
                .fallbacksDefault()
                .thinking(BetaThinkingConfigAdaptive.builder().build())
                .addTool(BetaWebSearchTool20260209.builder().maxUses(10L).build())
                .system(SYSTEM_PROMPT)
                .outputConfig(TeamResearch.class)
                .addUserMessage("Research the men's senior national football team of %s (team id \"%s\")."
                        .formatted(teamName, teamId));
    }

    // With web search the response interleaves searches, results and commentary; the
    // schema-constrained JSON is the last text block.
    private TeamResearch finalAnswer(StructuredMessage<TeamResearch> response, String teamId) {
        List<StructuredContentBlock<TeamResearch>> textBlocks = response.content().stream()
                .filter(StructuredContentBlock::isText)
                .toList();
        if (textBlocks.isEmpty()) {
            throw new TeamResearchException("Research for " + teamId + " returned no answer");
        }
        TeamResearch research = textBlocks.get(textBlocks.size() - 1).asText().text();
        if (research.coach() == null || research.squad() == null || research.squad().isEmpty()) {
            throw new TeamResearchException("Research for " + teamId + " returned no coach or squad");
        }
        return research;
    }
}
