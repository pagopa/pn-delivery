package it.pagopa.pn.delivery.exception;

public class PnDeliveryMessageNotFoundException extends PnNotFoundException {
    public PnDeliveryMessageNotFoundException(String message, String description, String errorcode) {
        super(message,description, errorcode);
    }
}
