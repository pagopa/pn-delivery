package it.pagopa.pn.delivery.middleware;

import java.time.Instant;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;

public interface NotificationViewedProducer extends MomProducer<PnDeliveryNotificationViewedEvent> {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-producer";

    default void sendNotificationViewed( String iun, Instant when, int recipientIndex ) {
    	PnDeliveryNotificationViewedEvent event = buildNotificationViewed( iun, when, recipientIndex );
        this.push( event );
    }

    private PnDeliveryNotificationViewedEvent buildNotificationViewed( String iun, Instant when, int recipientIndex ) {
        return PnDeliveryNotificationViewedEvent.builder()
                .header( StandardEventHeader.builder()
                        .iun( iun )
                        .eventId( iun + "_notification_viewed_" + recipientIndex )
                        .createdAt( when )
                        .eventType( EventType.NOTIFICATION_VIEWED.name() )
                        .publisher( EventPublisher.DELIVERY.name() )
                        .build()
                )
                .payload( PnDeliveryNotificationViewedEvent.Payload.builder()
                        .iun( iun )
                        .recipientIndex( recipientIndex )
                        .build()
                )
                .build();
    }
}
