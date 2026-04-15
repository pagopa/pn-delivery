package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class ValidationException extends PnValidationException {

    public ValidationException(List<ProblemError> problemErrorList, String message) {
        super( message, problemErrorList );
    }
}
