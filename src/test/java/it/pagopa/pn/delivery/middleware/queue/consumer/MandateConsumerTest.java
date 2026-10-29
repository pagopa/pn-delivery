package it.pagopa.pn.delivery.middleware.queue.consumer;

import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnMandateEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.svc.NotificationDelegatedService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(SpringExtension.class)
class MandateConsumerTest {

    @InjectMocks
    private MandateConsumer mandateConsumer;

    @Mock
    private NotificationDelegatedService notificationDelegatedService;

    /**
     * Method under test: {@link MandateConsumer#handleAcceptedMandate(Message)}
     */
    @Test
    void testPnDeliveryAcceptedMandateConsumer() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doNothing().when(notificationDelegatedService).handleAcceptedMandate(same(payload), eq(EventType.MANDATE_ACCEPTED));

        Consumer<Message<PnMandateEvent.Payload>> consumer = message -> mandateConsumer.handleAcceptedMandate(message);
        assertDoesNotThrow(() -> consumer.accept(new GenericMessage<>(payload)));
    }

    /**
     * Method under test: {@link MandateConsumer#handleAcceptedMandate(Message)}
     */
    @Test
    void testPnDeliveryAcceptedMandateConsumerException() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doThrow(PnInternalException.class).when(notificationDelegatedService).handleAcceptedMandate(any(), any());

        Consumer<Message<PnMandateEvent.Payload>> consumer = message -> mandateConsumer.handleAcceptedMandate(message);
        Message<PnMandateEvent.Payload> message = new GenericMessage<>(payload);
        assertThrows(PnInternalException.class, () -> consumer.accept(message));
    }

    @Test
    void testPnDeliveryUpdatedMandateConsumer() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doNothing().when(notificationDelegatedService).handleUpdatedMandate(same(payload), eq(EventType.MANDATE_UPDATED));

        Consumer<Message<PnMandateEvent.Payload>> consumer = message -> mandateConsumer.handleUpdatedMandate(message);
        assertDoesNotThrow(() -> consumer.accept(new GenericMessage<>(payload)));
    }

    @Test
    void testPnDeliveryUpdatedMandateConsumerException() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doThrow(PnInternalException.class).when(notificationDelegatedService).handleUpdatedMandate(any(), any());

        Consumer<Message<PnMandateEvent.Payload>> consumer = message -> mandateConsumer.handleUpdatedMandate(message);
        Message<PnMandateEvent.Payload> message = new GenericMessage<>(payload);
        assertThrows(PnInternalException.class, () -> consumer.accept(message));
    }


    /**
     * Method under test: {@link MandateConsumer#handleRejectedMandate(Message)}
     */
    @Test
    void testPnDeliveryRejectedMandateConsumer() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doNothing().when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(same(payload.getMandateId()), eq(EventType.MANDATE_REJECTED));

        Consumer<Message<PnMandateEvent.Payload>> consumer = message -> mandateConsumer.handleRejectedMandate(message);
        assertDoesNotThrow(() -> consumer.accept(new GenericMessage<>(payload)));
    }

    /**
     * Method under test: {@link MandateConsumer#handleRejectedMandate(Message)}
     */
    @Test
    void testPnDeliveryRejectedMandateConsumerException() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doThrow(PnInternalException.class).when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(any(), any());

        Consumer<Message<PnMandateEvent.Payload>> consumer = message -> mandateConsumer.handleRejectedMandate(message);
        Message<PnMandateEvent.Payload> message = new GenericMessage<>(payload);
        assertThrows(PnInternalException.class, () -> consumer.accept(message));
    }

    /**
     * Method under test: {@link MandateConsumer#handleRevokedMandate(Message)}
     */
    @Test
    void testPnDeliveryRevokedMandateConsumer() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doNothing().when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(same(payload.getMandateId()), eq(EventType.MANDATE_REVOKED));

        Consumer<Message<PnMandateEvent.Payload>> consumer = message -> mandateConsumer.handleRevokedMandate(message);
        assertDoesNotThrow(() -> consumer.accept(new GenericMessage<>(payload)));
    }

    /**
     * Method under test: {@link MandateConsumer#handleRevokedMandate(Message)}
     */
    @Test
    void testPnDeliveryRevokedMandateConsumerException() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doThrow(PnInternalException.class).when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(any(), any());

        Consumer<Message<PnMandateEvent.Payload>> consumer = message -> mandateConsumer.handleRevokedMandate(message);
        Message<PnMandateEvent.Payload> message = new GenericMessage<>(payload);
        assertThrows(PnInternalException.class, () -> consumer.accept(message));
    }

    /**
     * Method under test: {@link MandateConsumer#handleExpiredMandate(Message)}
     */
    @Test
    void testPnDeliveryExpiredMandateConsumer() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doNothing().when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(same(payload.getMandateId()), eq(EventType.MANDATE_EXPIRED));

        Consumer<Message<PnMandateEvent.Payload>> consumer = message -> mandateConsumer.handleExpiredMandate(message);
        assertDoesNotThrow(() -> consumer.accept(new GenericMessage<>(payload)));
    }

    /**
     * Method under test: {@link MandateConsumer#handleExpiredMandate(Message)}
     */
    @Test
    void testPnDeliveryExpiredMandateConsumerException() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doThrow(PnInternalException.class).when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(any(), any());

        Consumer<Message<PnMandateEvent.Payload>> consumer = message -> mandateConsumer.handleExpiredMandate(message);
        Message<PnMandateEvent.Payload> message = new GenericMessage<>(payload);
        assertThrows(PnInternalException.class, () -> consumer.accept(message));
    }
}

