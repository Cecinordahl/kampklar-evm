package no.kampklar.evm.controller;

import no.kampklar.evm.dto.ResultSuggestion;
import no.kampklar.evm.service.ResultSuggestionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/groups")
public class AdminGroupController {

    private final ResultSuggestionService resultSuggestionService;

    public AdminGroupController(ResultSuggestionService resultSuggestionService) {
        this.resultSuggestionService = resultSuggestionService;
    }

    /**
     * "Hent resultater med AI". POST because it is slow (1-3 minutes) and paid, though it saves
     * nothing: the suggestions are reviewed and saved one match at a time.
     *
     * <p>{@code matchIds} limits the research to those matches (e.g. one country's, or one
     * match), so the admin only pays for what they need; omitted means every kicked-off match.
     */
    @PostMapping("/{groupId}/result-suggestions")
    public List<ResultSuggestion> suggestResults(@PathVariable String groupId,
                                                 @RequestBody(required = false) SuggestionScope scope) {
        return resultSuggestionService.suggest(groupId, scope == null ? null : scope.matchIds());
    }

    public record SuggestionScope(List<String> matchIds) {
    }
}
