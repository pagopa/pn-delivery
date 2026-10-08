package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_GROUPS_UNAVAILABLE;

public class PnDeliveryGroupsUnavailableException extends PnRuntimeException {

    public PnDeliveryGroupsUnavailableException(String description) {
        super( "Unable to retrieve PA groups",
                description,
                HttpStatus.BAD_GATEWAY.value(),
                ERROR_CODE_DELIVERY_GROUPS_UNAVAILABLE,
                null,
                null);
    }
}