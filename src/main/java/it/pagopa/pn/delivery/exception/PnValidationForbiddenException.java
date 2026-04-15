package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

public class PnValidationForbiddenException extends PnRuntimeException {

    public PnValidationForbiddenException(String message, String description, String errorCode, String detail, String element) {
        super(message, description, HttpStatus.FORBIDDEN.value(), errorCode, element, detail);
    }
}
