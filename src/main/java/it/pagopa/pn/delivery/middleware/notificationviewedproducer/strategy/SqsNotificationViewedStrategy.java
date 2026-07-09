package it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.delivery.middleware.notificationviewedproducer.NotificationViewedEventStrategy;
import it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy.producer.NotificationViewedProducer;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("sqsNotificationViewedStrategy")
public class SqsNotificationViewedStrategy implements NotificationViewedEventStrategy {

    private final NotificationViewedProducer producer;

    public SqsNotificationViewedStrategy(NotificationViewedProducer producer) {
        this.producer = producer;
    }

    @Override
    public void sendNotificationViewed(
            String iun, Instant when, int recipientIndex,
            NotificationViewDelegateInfo delegateInfo,
            String sourceChannel, String sourceChannelDetails) {
        producer.sendNotificationViewed(
                iun, when, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails
        );
    }
}
