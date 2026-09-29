package no.kampklar.evm.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamResearchServiceTest {

    @Test
    void refusesToResearchWithoutAnApiKeyInsteadOfCallingTheApi() {
        TeamResearchService service = new TeamResearchService(new ClaudeResearchClient(""));

        assertThatThrownBy(() -> service.research("norway", "Norge"))
                .isInstanceOf(ResearchUnavailableException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }

    @Test
    void buildsARequestWhoseStructuredOutputSchemaPassesTheSdksValidation() {
        // Catches an unsupported schema shape in TeamResearch before it costs a paid API call.
        var params = TeamResearchService.buildRequest("norway", "Norge").build();

        assertThat(params.rawParams().model().asString()).isEqualTo("claude-opus-5");
    }
}
