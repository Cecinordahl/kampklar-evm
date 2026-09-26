package no.kampklar.evm.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMatchException extends RuntimeException {

    public InvalidMatchException(String message) {
        super(message);
    }
}
