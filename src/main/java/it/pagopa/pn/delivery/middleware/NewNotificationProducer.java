package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.api.dto.events.*;

import java.time.Instant;

public interface NewNotificationProducer extends MomProducer<PnDeliveryNewNotificationEvent> {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-producer";

    default void sendNewNotificationEvent( String paId, String iun, Instant when) {
        PnDeliveryNewNotificationEvent event = buildNewNotificationEvent(iun, paId, when );
        this.push( event );
    }

    private PnDeliveryNewNotificationEvent buildNewNotificationEvent(String iun, String paId, Instant when) {
        return PnDeliveryNewNotificationEvent.builder()
                .header( StandardEventHeader.builder()
                        .iun( iun )
                        .eventId( iun + "_start" )
                        .createdAt( when )
                        .eventType( EventType.NEW_NOTIFICATION.name() )
                        .publisher( EventPublisher.DELIVERY.name() )
                        .build()
                )
                .payload( PnDeliveryNewNotificationEvent.Payload.builder()
                        .paId( paId )
                        .build()
                )
                .build();
    }
}
