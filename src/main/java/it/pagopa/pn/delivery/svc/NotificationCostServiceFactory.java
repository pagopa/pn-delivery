package it.pagopa.pn.delivery.svc;

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
        if (deliveryConfigs.getNewCostMsActivationDate() == null || sentAt.isBefore(deliveryConfigs.getNewCostMsActivationDate())) {
            return deliveryPushNotificationCostService;
        } else {
            return notificationCostService;
        }
    }
}
