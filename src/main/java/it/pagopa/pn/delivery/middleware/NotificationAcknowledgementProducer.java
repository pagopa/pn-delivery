package it.pagopa.pn.delivery.middleware;

import java.time.Instant;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationAcknowledgementEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;

public interface NotificationAcknowledgementProducer extends MomProducer<PnDeliveryNotificationAcknowledgementEvent> {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-producer";

    default void sendNotificationAcknowlwdgement( String iun, Instant when, int recipientIndex ) {
    	PnDeliveryNotificationAcknowledgementEvent event = buildNotificationAcknowledgementEvent( iun, when, recipientIndex );
        this.push( event );
    }

    private PnDeliveryNotificationAcknowledgementEvent buildNotificationAcknowledgementEvent( String iun, Instant when, int recipientIndex ) {
        return PnDeliveryNotificationAcknowledgementEvent.builder()
                .header( StandardEventHeader.builder()
                        .iun( iun )
                        .eventId( iun + "_notification_viewed_" + recipientIndex )
                        .createdAt( when )
                        .eventType( EventType.NOTIFICATION_VIEWED.name() )
                        .publisher( EventPublisher.DELIVERY.name() )
                        .build()
                )
                .payload( PnDeliveryNotificationAcknowledgementEvent.Payload.builder()
                        .iun( iun )
                        .recipientIndex( recipientIndex )
                        .build()
                )
                .build();
    }
}
