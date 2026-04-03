package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class NotificationCostServiceFactory {

    private final PnDeliveryConfigs deliveryConfigs;
    private final DeliveryPushNotificationCostService deliveryPushNotificationCostService;
    private final NotificationCostServiceImpl notificationCostService;

    public NotificationCostService getNotificationCostServiceBySentAt(Instant sentAt) {
        if (sentAt == null) {
            throw new PnInternalException("SentAt cannot be null", "PN_GENERIC_ERROR");
        }
        if (deliveryConfigs.getNewCostMsActivationDate() == null || sentAt.isBefore(deliveryConfigs.getNewCostMsActivationDate())) {
            return deliveryPushNotificationCostService;
        } else {
            return notificationCostService;
        }
    }
}
