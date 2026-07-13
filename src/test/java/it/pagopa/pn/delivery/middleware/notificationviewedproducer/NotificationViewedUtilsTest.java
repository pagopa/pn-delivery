package it.pagopa.pn.delivery.middleware.notificationviewedproducer;

import it.pagopa.pn.api.dto.events.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NotificationViewedUtilsTest {

    private static final String IUN = "TEST-IUN-001";
    private static final Instant WHEN = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void payloadAndHeaderContainsAllProvidedFields() {
        int recipientIndex = 3;
        NotificationViewDelegateInfo delegateInfo = NotificationViewDelegateInfo.builder().build();
        String sourceChannel = "WEB";
        String sourceChannelDetails = "BROWSER";

        PnDeliveryNotificationViewedEvent event = NotificationViewedUtils.buildNotificationViewed(
                IUN, WHEN, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails);

        PnDeliveryNotificationViewedEvent.Payload payload = event.getPayload();
        assertEquals(IUN, payload.getIun());
        assertEquals(recipientIndex, payload.getRecipientIndex());
        assertEquals(delegateInfo, payload.getDelegateInfo());
        assertEquals(sourceChannel, payload.getSourceChannel());
        assertEquals(sourceChannelDetails, payload.getSourceChannelDetails());
        assertEquals(WHEN, payload.getViewedDate());

        String expectedEventId = IUN + "_notification_viewed_rec" + recipientIndex;
        assertEquals(expectedEventId, event.getMessageDeduplicationId());
        assertEquals(expectedEventId, event.getMessageGroupId());

        StandardEventHeader header = event.getHeader();
        assertEquals(IUN, header.getIun());
        assertEquals(expectedEventId, header.getEventId());
        assertEquals(WHEN, header.getCreatedAt());
        assertEquals(EventType.NOTIFICATION_VIEWED.name(), header.getEventType());
        assertEquals(EventPublisher.DELIVERY.name(), header.getPublisher());
    }

}