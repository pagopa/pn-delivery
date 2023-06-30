package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

public class PnForbiddenException extends PnRuntimeException {

    public PnForbiddenException(String message) {
        super("Accesso negato!", "L'utente non è autorizzato ad accedere alla risorsa richiesta.",
                HttpStatus.NOT_FOUND.value(), message, null, null);
    }

}
