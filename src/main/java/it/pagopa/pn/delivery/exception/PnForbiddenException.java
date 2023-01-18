package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_FORBIDDEN;

public class PnForbiddenException extends PnRuntimeException {

    public PnForbiddenException() {
        super("Accesso negato!", "L'utente non è autorizzato ad accedere alla risorsa richiesta.",
                HttpStatus.FORBIDDEN.value(), ERROR_CODE_FORBIDDEN, null, null);
    }

}
