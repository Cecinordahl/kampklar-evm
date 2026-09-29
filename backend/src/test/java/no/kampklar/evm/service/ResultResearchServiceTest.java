package no.kampklar.evm.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultResearchServiceTest {

    @Test
    void buildsARequestWhoseStructuredOutputSchemaPassesTheSdksValidation() {
        // Catches an unsupported schema shape in ResultResearch before it costs a paid API call.
        var fixture = new ResultResearchService.Fixture("m1", "Norge", "Spania",
                Instant.parse("2026-09-24T18:45:00Z"), List.of("Martin Ødegaard"), List.of("Pedri"));

        var params = ResultResearchService.buildRequest("Gruppe A1", List.of(fixture)).build();

        assertThat(params.rawParams().model().asString()).isEqualTo("claude-opus-5-5");
    }
}
