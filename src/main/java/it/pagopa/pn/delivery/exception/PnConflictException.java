package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.http.HttpStatus;

public class PnConflictException extends PnInternalException {

    public PnConflictException(String errorCode, String message) {
        super(message, HttpStatus.CONFLICT.value(), errorCode);
    }
}
