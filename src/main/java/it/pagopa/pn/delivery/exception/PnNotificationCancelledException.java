package it.pagopa.pn.delivery.exception;

import lombok.Getter;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATION_CANCELLED;

@Getter
public class PnNotificationCancelledException extends PnNotFoundException {

    public PnNotificationCancelledException(String description, Exception ex) {
        super("Notification cancelled", description, ERROR_CODE_DELIVERY_NOTIFICATION_CANCELLED, ex);
    }
}
