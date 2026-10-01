package no.kampklar.evm.controller;

import no.kampklar.evm.dto.PlayerEditRequest;
import no.kampklar.evm.service.ManualEditService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/players")
public class AdminPlayerController {

    private final ManualEditService manualEditService;

    public AdminPlayerController(ManualEditService manualEditService) {
        this.manualEditService = manualEditService;
    }

    /** Manual correction from admin mode; the page reads the result back from Firestore. */
    @PutMapping("/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void edit(@PathVariable String playerId, @RequestBody PlayerEditRequest request) {
        manualEditService.editPlayer(playerId, request);
    }
}
