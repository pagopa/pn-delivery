package it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.delivery.middleware.notificationviewedproducer.NotificationViewedEventStrategy;
import it.pagopa.pn.delivery.middleware.eventbridge.EventBridgeProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static it.pagopa.pn.delivery.middleware.notificationviewedproducer.NotificationViewedUtils.buildNotificationViewed;

@Component("eventBridgeNotificationViewedStrategy")
@RequiredArgsConstructor
public class EventBridgeNotificationViewedStrategy implements NotificationViewedEventStrategy {
    private final EventBridgeProducer<PnDeliveryNotificationViewedEvent> producer;

    @Override
    public void sendNotificationViewed(
            String iun, Instant when, int recipientIndex,
            NotificationViewDelegateInfo delegateInfo,
            String sourceChannel, String sourceChannelDetails) {

        PnDeliveryNotificationViewedEvent event = buildNotificationViewed(
                iun, when, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails
        );

        producer.sendEvent(event);
    }
}
