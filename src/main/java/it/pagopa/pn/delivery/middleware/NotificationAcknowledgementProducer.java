package it.pagopa.pn.delivery.middleware;

import java.time.Instant;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationAcknowledgementEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;

public interface NotificationAcknowledgementProducer extends MomProducer<PnDeliveryNotificationAcknowledgementEvent> {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-producer";

    default void sendAcknowledgedNotificationEvent( String iun, Instant when ) {
    	PnDeliveryNotificationAcknowledgementEvent event = buildAcknowledgedNotificationEvent( iun, when );
        this.push( event );
    }

    private PnDeliveryNotificationAcknowledgementEvent buildAcknowledgedNotificationEvent( String iun, Instant when ) {
        return PnDeliveryNotificationAcknowledgementEvent.builder()
                .header( StandardEventHeader.builder()
                        .iun( iun )
                        .eventId( iun + "_acknoledgement" )
                        .createdAt( when )
                        .eventType( EventType.NEW_NOTIFICATION.name() )
                        .publisher( EventPublisher.DELIVERY.name() )
                        .build()
                )
                .payload( PnDeliveryNotificationAcknowledgementEvent.Payload.builder()
                        .iun( iun )
                        .build()
                )
                .build();
    }
}
