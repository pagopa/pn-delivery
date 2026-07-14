package it.pagopa.pn.delivery.middleware.notificationviewedproducer;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;

import java.time.Instant;

public interface NotificationViewedEventStrategy {
    void sendNotificationViewed(
            String iun,
            Instant when,
            int recipientIndex,
            NotificationViewDelegateInfo delegateInfo,
            String sourceChannel,
            String sourceChannelDetails
    );
}