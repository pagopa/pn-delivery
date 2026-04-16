package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import org.springframework.http.HttpStatus;

import java.util.List;

public class ValidationException extends PnRuntimeException {

    public ValidationException(List<ProblemError> problemErrorList, String message, HttpStatus code) {
        super(code.getReasonPhrase(), message, code.value(), problemErrorList);
    }
}
