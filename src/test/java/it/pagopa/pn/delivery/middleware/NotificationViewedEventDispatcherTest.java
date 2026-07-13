package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.delivery.middleware.notificationviewedproducer.NotificationViewedEventStrategy;
import it.pagopa.pn.delivery.models.internal.notification.CommunicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationViewedEventDispatcherTest {

    @Mock
    private NotificationViewedEventStrategy sqsStrategy;

    @Mock
    private NotificationViewedEventStrategy eventBridgeStrategy;

    private NotificationViewedEventDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationViewedEventDispatcher(sqsStrategy, eventBridgeStrategy);
    }

    @Test
    void legalCommunicationType_routesToSqsStrategy() {
        String iun = "IUN-TEST-001";
        Instant when = Instant.now();
        int recipientIndex = 0;
        NotificationViewDelegateInfo delegateInfo = NotificationViewDelegateInfo.builder()
                .internalId("delegate-id").build();
        String sourceChannel = "WEB";
        String sourceChannelDetails = "some-detail";

        dispatcher.sendNotificationViewed(iun, when, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails, CommunicationType.LEGAL);

        verify(sqsStrategy).sendNotificationViewed(iun, when, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails);
        verifyNoInteractions(eventBridgeStrategy);
    }

    @Test
    void informalCommunicationType_routesToEventBridgeStrategy() {
        String iun = "IUN-TEST-002";
        Instant when = Instant.now();
        int recipientIndex = 1;
        NotificationViewDelegateInfo delegateInfo = null;
        String sourceChannel = "APP_IO";
        String sourceChannelDetails = "some-detail";

        dispatcher.sendNotificationViewed(iun, when, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails, CommunicationType.INFORMAL);

        verify(eventBridgeStrategy).sendNotificationViewed(iun, when, recipientIndex, delegateInfo, sourceChannel, sourceChannelDetails);
        verifyNoInteractions(sqsStrategy);
    }
}