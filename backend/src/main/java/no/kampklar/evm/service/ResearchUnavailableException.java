package no.kampklar.evm.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ResearchUnavailableException extends RuntimeException {

    public ResearchUnavailableException(String message) {
        super(message);
    }
}
