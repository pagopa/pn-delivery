package it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.delivery.middleware.eventbridge.EventBridgeProducer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventBridgeNotificationViewedStrategyTest {

    private static final String IUN = "TEST-IUN-001";
    private static final Instant WHEN = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    @SuppressWarnings("unchecked")
    void producerIsCalledExactlyOnce() {
        EventBridgeProducer<PnDeliveryNotificationViewedEvent> producer = mock(EventBridgeProducer.class);
        EventBridgeNotificationViewedStrategy strategy = new EventBridgeNotificationViewedStrategy(producer);

        strategy.sendNotificationViewed(IUN, WHEN, 0, null, "WEB", null);

        verify(producer, times(1)).sendEvent((PnDeliveryNotificationViewedEvent) any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void producerReceivesEventWithCorrectPayload() {
        EventBridgeProducer<PnDeliveryNotificationViewedEvent> producer = mock(EventBridgeProducer.class);
        EventBridgeNotificationViewedStrategy strategy = new EventBridgeNotificationViewedStrategy(producer);
        NotificationViewDelegateInfo delegateInfo = NotificationViewDelegateInfo.builder().build();
        int recipientIndex = 2;
        String sourceChannel = "WEB";
        String sourceChannelDetails = "BROWSER";
        ArgumentCaptor<PnDeliveryNotificationViewedEvent> captor = ArgumentCaptor.forClass(PnDeliveryNotificationViewedEvent.class);

        strategy.sendNotificationViewed(IUN, WHEN, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails);

        verify(producer).sendEvent(captor.capture());
        PnDeliveryNotificationViewedEvent.Payload payload = captor.getValue().getPayload();
        assertEquals(IUN, payload.getIun());
        assertEquals(recipientIndex, payload.getRecipientIndex());
        assertEquals(delegateInfo, payload.getDelegateInfo());
        assertEquals(sourceChannel, payload.getSourceChannel());
        assertEquals(sourceChannelDetails, payload.getSourceChannelDetails());
        assertEquals(WHEN, payload.getViewedDate());
    }


}