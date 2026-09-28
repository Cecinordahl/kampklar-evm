package no.kampklar.evm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated liveness check. The admin view pings it on load to wake the backend
 * (Render's free tier sleeps when idle) and show a progress indicator until it answers.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.noContent().build();
    }
}
