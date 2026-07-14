package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.delivery.middleware.notificationviewedproducer.NotificationViewedEventStrategy;
import it.pagopa.pn.delivery.models.internal.notification.CommunicationType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NotificationViewedEventDispatcher {
    private final NotificationViewedEventStrategy sqsStrategy;
    private final NotificationViewedEventStrategy eventBridgeStrategy;

    public NotificationViewedEventDispatcher(
            @Qualifier("sqsNotificationViewedStrategy") NotificationViewedEventStrategy sqsStrategy,
            @Qualifier("eventBridgeNotificationViewedStrategy") NotificationViewedEventStrategy eventBridgeStrategy) {
        this.sqsStrategy = sqsStrategy;
        this.eventBridgeStrategy = eventBridgeStrategy;
    }

    public void sendNotificationViewed(
            String iun, Instant when, int recipientIndex,
            NotificationViewDelegateInfo delegateInfo,
            String sourceChannel, String sourceChannelDetails,
            CommunicationType communicationType) {

        NotificationViewedEventStrategy strategy = communicationType == CommunicationType.LEGAL
                ? sqsStrategy
                : eventBridgeStrategy;

        strategy.sendNotificationViewed(
                iun, when, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails
        );
    }
}