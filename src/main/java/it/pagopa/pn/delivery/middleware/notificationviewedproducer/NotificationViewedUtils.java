package it.pagopa.pn.delivery.middleware.notificationviewedproducer;

import it.pagopa.pn.api.dto.events.*;

import java.time.Instant;

public class NotificationViewedUtils {
    private NotificationViewedUtils() {
        // utility class, prevent instantiation
    }

    public static PnDeliveryNotificationViewedEvent buildNotificationViewed(String iun, Instant when, int recipientIndex, NotificationViewDelegateInfo delegateInfo, String sourceChannel, String sourceChannelDetails) {
        String eventId = iun + "_notification_viewed_rec" + recipientIndex;
        return PnDeliveryNotificationViewedEvent.builder()
                .messageDeduplicationId(eventId)
                .messageGroupId(eventId)
                .header( StandardEventHeader.builder()
                        .iun( iun )
                        .eventId( eventId)
                        .createdAt( when )
                        .eventType( EventType.NOTIFICATION_VIEWED.name() )
                        .publisher( EventPublisher.DELIVERY.name() )
                        .build()
                )
                .payload( PnDeliveryNotificationViewedEvent.Payload.builder()
                        .iun( iun )
                        .recipientIndex( recipientIndex )
                        .delegateInfo( delegateInfo )
                        .sourceChannel( sourceChannel )
                        .sourceChannelDetails( sourceChannelDetails )
                        .viewedDate( when )
                        .build()
                )
                .build();
    }
}
