package it.pagopa.pn.delivery.exception;

import lombok.Getter;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_MANDATENOTFOUND;

@Getter
public class PnMandateNotFoundException extends PnNotFoundException {

    public PnMandateNotFoundException(String description) {
        super("Mandate not found", description, ERROR_CODE_DELIVERY_MANDATENOTFOUND);
    }

}
