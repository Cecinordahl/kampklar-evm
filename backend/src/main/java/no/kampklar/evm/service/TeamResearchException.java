package no.kampklar.evm.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The Claude API call failed or returned something unusable - nothing was written. */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class TeamResearchException extends RuntimeException {

    public TeamResearchException(String message) {
        super(message);
    }

    public TeamResearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
