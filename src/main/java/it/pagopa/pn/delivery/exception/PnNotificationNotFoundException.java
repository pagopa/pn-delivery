package it.pagopa.pn.delivery.exception;

import lombok.Getter;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND;

@Getter
public class PnNotificationNotFoundException extends PnNotFoundException {

    public PnNotificationNotFoundException(String description) {
        super("Notification not found", description, ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
    }

}
