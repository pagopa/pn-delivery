package it.pagopa.pn.delivery.middleware;

import java.time.Instant;

import it.pagopa.pn.api.dto.events.*;

public interface NotificationViewedProducer extends MomProducer<PnDeliveryNotificationViewedEvent> {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-producer";


    default void sendNotificationViewed( String iun, Instant when, int recipientIndex, NotificationViewDelegateInfo delegateInfo ) {
    	PnDeliveryNotificationViewedEvent event = buildNotificationViewed( iun, when, recipientIndex, delegateInfo );
        this.push( event );
    }

    private PnDeliveryNotificationViewedEvent buildNotificationViewed( String iun, Instant when, int recipientIndex, NotificationViewDelegateInfo delegateInfo ) {
        String eventId = iun + "_notification_viewed_rec" + recipientIndex;
        return PnDeliveryNotificationViewedEvent.builder()
                .messageDeduplicationId(eventId)
                .messageGroupId("delivery")
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
                        .build()
                )
                .build();
    }
}
