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
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * The one place that calls Claude: a web-search research request whose answer is constrained
 * to a record type (structured output - no free-text parsing). These are the only paid calls in
 * the stack, so each call's usage is logged.
 */
@Component
public class ClaudeResearchClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeResearchClient.class);

    static final String MODEL = "claude-opus-5";
    // Web search runs a server-side loop that may hand back a paused turn to be resumed.
    private static final int MAX_CONTINUATIONS = 5;

    private final AnthropicClient client;

    public ClaudeResearchClient(@Value("${anthropic.api-key}") String apiKey) {
        // No key configured (e.g. `mvn test`): the client exists but refuses to research.
        this.client = apiKey == null || apiKey.isBlank() ? null : AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofMinutes(10)) // several searches plus thinking can take minutes
                .build();
    }

    /** A research request with the shared settings; callers add the user message. */
    static <T> StructuredMessageCreateParams.Builder<T> requestBuilder(
            String systemPrompt, Class<T> answerType, long maxSearches) {
        return MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(16000L)
                // Re-runs a (rare) safety-classifier refusal on Anthropic's recommended fallback model.
                .addBeta("server-side-fallback-2026-07-01")
                .fallbacksDefault()
                .thinking(BetaThinkingConfigAdaptive.builder().build())
                .addTool(BetaWebSearchTool20260209.builder().maxUses(maxSearches).build())
                .system(systemPrompt)
                .outputConfig(answerType);
    }

    /** Runs the request to completion; {@code label} names the subject in logs and errors. */
    <T> T research(StructuredMessageCreateParams.Builder<T> request, String label) {
        if (client == null) {
            throw new ResearchUnavailableException("ANTHROPIC_API_KEY is not configured");
        }
        try {
            for (int attempt = 0; attempt <= MAX_CONTINUATIONS; attempt++) {
                StructuredMessage<T> response = client.beta().messages().create(request.build());
                log.info("Research for {} (call {}): usage {}", label, attempt + 1, response.usage());

                BetaStopReason stopReason = response.stopReason().orElse(null);
                if (BetaStopReason.PAUSE_TURN.equals(stopReason)) {
                    // Resend with the paused assistant turn appended; the server resumes the search loop.
                    request.addMessage(response.rawMessage());
                    continue;
                }
                if (BetaStopReason.REFUSAL.equals(stopReason)) {
                    throw new ResearchException("Claude declined to research " + label
                            + response.rawMessage().stopDetails().map(d -> " (" + d.category() + ")").orElse(""));
                }
                if (BetaStopReason.MAX_TOKENS.equals(stopReason)) {
                    throw new ResearchException("Research answer for " + label + " was cut off");
                }
                return finalAnswer(response, label);
            }
        } catch (AnthropicException e) {
            throw new ResearchException("Claude API call failed for " + label + ": " + e.getMessage(), e);
        }
        throw new ResearchException("Research for " + label + " did not finish after "
                + MAX_CONTINUATIONS + " continuations");
    }

    // With web search the response interleaves searches, results and commentary; the
    // schema-constrained JSON is the last text block.
    private static <T> T finalAnswer(StructuredMessage<T> response, String label) {
        List<StructuredContentBlock<T>> textBlocks = response.content().stream()
                .filter(StructuredContentBlock::isText)
                .toList();
        if (textBlocks.isEmpty()) {
            throw new ResearchException("Research for " + label + " returned no answer");
        }
        return textBlocks.get(textBlocks.size() - 1).asText().text();
    }
}
