package it.pagopa.pn.delivery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class PnBadRequestException extends RuntimeException {
    public PnBadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
    public PnBadRequestException(String message) {
        super(message);
    }
}
