package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.delivery.models.AsseverationEvent;
import it.pagopa.pn.delivery.models.InternalAsseverationEvent;

public interface AsseverationEventsProducer extends MomProducer<AsseverationEvent> {

    default void sendAsseverationEvent( InternalAsseverationEvent internalAsseverationEvent ) {
        AsseverationEvent asseverationEvent = buildAsseverationEvent(internalAsseverationEvent);
        this.push( asseverationEvent );
    }

    default AsseverationEvent buildAsseverationEvent(InternalAsseverationEvent internalAsseverationEvent) {
        String eventId = internalAsseverationEvent.getIun() + "asseveration_event" + internalAsseverationEvent.getSenderPaId();
        return AsseverationEvent.builder()
                .header( StandardEventHeader.builder()
                        .iun( internalAsseverationEvent.getIun() )
                        .eventId( eventId )
                        .eventType( "ASSEVERATION_EVENT" )
                        .publisher( EventPublisher.DELIVERY.name())
                        .build()
                )
                .payload( AsseverationEvent.Payload.builder()
                        .iun( internalAsseverationEvent.getIun() )
                        .senderPaId( internalAsseverationEvent.getSenderPaId() )
                        .creditorTaxId( internalAsseverationEvent.getCreditorTaxId() )
                        .noticeCode( internalAsseverationEvent.getNoticeCode() )
                        .notificationSentAt( internalAsseverationEvent.getNotificationSentAt() )
                        .debitorPosUpdateDate( internalAsseverationEvent.getDebitorPosUpdateDate() )
                        .recordCreationDate( internalAsseverationEvent.getRecordCreationDate() )
                        .recipientId( internalAsseverationEvent.getRecipientId() )
                        .recipientType( internalAsseverationEvent.getRecipientType() )
                        .recipientIdx( internalAsseverationEvent.getRecipientIdx() )
                        .version( internalAsseverationEvent.getVersion() )
                        .asseverationEventPayload( internalAsseverationEvent.getAsseverationEventPayload() )
                        .build()
                )
                .build();
    }
}
