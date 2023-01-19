package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.delivery.models.InternalPaymentEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public interface PaymentEventsProducer extends MomProducer<PnDeliveryPaymentEvent> {

    default void sendPaymentEvents( List<InternalPaymentEvent> internalPaymentEvents ) {
        List<PnDeliveryPaymentEvent> events = buildPaymentEvents( internalPaymentEvents );
        this.push( events );
    }

    default List<PnDeliveryPaymentEvent> buildPaymentEvents(List<InternalPaymentEvent> internalPaymentEvents) {
        List<PnDeliveryPaymentEvent> paymentEvents = new ArrayList<>(internalPaymentEvents.size());
        for ( InternalPaymentEvent internalPaymentEvent : internalPaymentEvents ) {
            String eventId = internalPaymentEvent.getIun() + "_notification_paid_" + internalPaymentEvent.getRecipientIdx();
            paymentEvents.add( PnDeliveryPaymentEvent.builder()
                    .messageDeduplicationId( eventId )
                    .messageGroupId("delivery")
                    .header( StandardEventHeader.builder()
                            .iun( internalPaymentEvent.getIun() )
                            .eventId( eventId )
                            .createdAt( Instant.now() )
                            .eventType( EventType.NOTIFICATION_PAID.name() )
                            .publisher( EventPublisher.DELIVERY.name())
                            .build()
                    )
                    .payload( PnDeliveryPaymentEvent.Payload.builder()
                            .iun( internalPaymentEvent.getIun() )
                            .paymentType( internalPaymentEvent.getPaymentType() )
                            .paymentDate( internalPaymentEvent.getPaymentDate() )
                            .recipientIdx( internalPaymentEvent.getRecipientIdx() )
                            .recipientType( internalPaymentEvent.getRecipientType() )
                            .creditorTaxId( internalPaymentEvent.getCreditorTaxId() )
                            .noticeCode( internalPaymentEvent.getNoticeCode() )
                            .build()
                    )
                    .build()
            );
        }
        return paymentEvents;
    }
}
