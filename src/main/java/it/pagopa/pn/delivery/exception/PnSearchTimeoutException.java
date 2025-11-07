package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_SEARCH_TIMEOUT;

public class PnSearchTimeoutException extends PnRuntimeException {

    public PnSearchTimeoutException(String description) {
        super("Error for timeout", description, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_DELIVERY_SEARCH_TIMEOUT, null, null);
    }
}
