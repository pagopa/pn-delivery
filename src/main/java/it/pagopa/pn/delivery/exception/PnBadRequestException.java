package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PnBadRequestException extends PnRuntimeException {

    public PnBadRequestException(String message, String description, String errorcode) {
        super(message, description, HttpStatus.BAD_REQUEST.value(), errorcode, null, null);
    }

    public PnBadRequestException(String message, String description, String errorcode, String detail) {
        super(message, description, HttpStatus.BAD_REQUEST.value(), errorcode, null, detail);
    }

    public PnBadRequestException(String message, String description, String errorcode, Exception cause) {
        super(message, description, HttpStatus.BAD_REQUEST.value(), errorcode, null, null, cause);
    }

}
