package it.pagopa.pn.delivery.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class PnNotFoundException extends RuntimeException {
    public PnNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public PnNotFoundException(String message) {
        super(message);
    }
}
