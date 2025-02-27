package it.pagopa.pn.delivery.middleware;

import java.time.Instant;

import it.pagopa.pn.api.dto.events.*;

public interface NotificationViewedProducer extends MomProducer<PnDeliveryNotificationViewedEvent> {

    default void sendNotificationViewed( String iun, Instant when, int recipientIndex, NotificationViewDelegateInfo delegateInfo, String sourceChannel, String sourceChannelDetails) {
    	PnDeliveryNotificationViewedEvent event = buildNotificationViewed( iun, when, recipientIndex, delegateInfo, sourceChannel,sourceChannelDetails );
        this.push( event );
    }

    default PnDeliveryNotificationViewedEvent buildNotificationViewed( String iun, Instant when, int recipientIndex, NotificationViewDelegateInfo delegateInfo, String sourceChannel, String sourceChannelDetails) {
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
                        .build()
                )
                .build();
    }
}
