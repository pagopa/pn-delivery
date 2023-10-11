package it.pagopa.pn.delivery.exception;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_ROOTIDNOTFOUND;

public class PnRootIdNonFountException extends PnNotFoundException{

    public PnRootIdNonFountException(String description) {
        super("RootId not found", description, ERROR_CODE_DELIVERY_ROOTIDNOTFOUND);
    }
}
