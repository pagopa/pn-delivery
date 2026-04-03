package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.notificationcost.PnNotificationCostServiceClientImpl;
import it.pagopa.pn.delivery.pnclient.timelineservice.PnTimelineServiceClientImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class NotificationCostServiceFactory {
    private final PnDeliveryConfigs deliveryConfigs;
    private final PnDeliveryPushClientImpl pnDeliveryPushClient;
    private final PnTimelineServiceClientImpl pnTimelineServiceClient;
    private final PnNotificationCostServiceClientImpl pnNotificationCostServiceClient;
    private final NotificationProcessCostResponseMapper notificationMapper;

    public NotificationCostService getNotificationCostServiceBySentAt(Instant sentAt) {
        if (deliveryConfigs.getNewCostMsActivationDate() == null || sentAt.isBefore(deliveryConfigs.getNewCostMsActivationDate())) {
            return new DeliveryPushNotificationCostService(pnDeliveryPushClient, notificationMapper);
        } else {
            return new NotificationCostServiceImpl(pnTimelineServiceClient, pnNotificationCostServiceClient, notificationMapper);
        }
    }
}
