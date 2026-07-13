package it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy.producer.NotificationViewedProducer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SqsNotificationViewedStrategyTest {

    private static final String IUN = "TEST-IUN-001";
    private static final Instant WHEN = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void producerReceivesAllArgumentsUnchanged() {
        NotificationViewedProducer producer = mock(NotificationViewedProducer.class);
        SqsNotificationViewedStrategy strategy = new SqsNotificationViewedStrategy(producer);
        NotificationViewDelegateInfo delegateInfo = NotificationViewDelegateInfo.builder().build();
        int recipientIndex = 3;
        String sourceChannel = "WEB";
        String sourceChannelDetails = "BROWSER";

        strategy.sendNotificationViewed(IUN, WHEN, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails);

        verify(producer).sendNotificationViewed(IUN, WHEN, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails);
    }
}