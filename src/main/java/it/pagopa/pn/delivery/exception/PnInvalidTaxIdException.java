package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

public class PnInvalidTaxIdException extends PnRuntimeException {
    public PnInvalidTaxIdException(String errorCode) {
        super("Accesso negato!","Il codice fiscale del mittente risulta incongruente",
                HttpStatus.FORBIDDEN.value(), errorCode, null, null);
    }
}
