package it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy.producer;

import java.time.Instant;

import it.pagopa.pn.api.dto.events.*;

import static it.pagopa.pn.delivery.middleware.notificationviewedproducer.NotificationViewedUtils.buildNotificationViewed;

/**
 * Producer interface for sending notification viewed events via SQS.
 */
public interface NotificationViewedProducer extends MomProducer<PnDeliveryNotificationViewedEvent> {

    default void sendNotificationViewed( String iun, Instant when, int recipientIndex, NotificationViewDelegateInfo delegateInfo, String sourceChannel, String sourceChannelDetails) {
    	PnDeliveryNotificationViewedEvent event = buildNotificationViewed( iun, when, recipientIndex, delegateInfo, sourceChannel,sourceChannelDetails );
        this.push( event );
    }
}
