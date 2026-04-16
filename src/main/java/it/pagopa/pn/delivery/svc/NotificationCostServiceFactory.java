package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.utils.FeatureFlagUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class NotificationCostServiceFactory {

    private final DeliveryPushNotificationCostService deliveryPushNotificationCostService;
    private final NotificationCostServiceImpl notificationCostServiceImpl;
    private final FeatureFlagUtils featureFlagUtils;

    public NotificationCostService getNotificationCostServiceBySentAt(Instant sentAt) {
        if (sentAt == null) {
            throw new PnInternalException("SentAt cannot be null", "PN_GENERIC_ERROR");
        }

        if(featureFlagUtils.isIntegrationWithNewCostServiceEnabled(sentAt)) {
            return notificationCostServiceImpl;
        } else {
            return deliveryPushNotificationCostService;
        }
    }
}
