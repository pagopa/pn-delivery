package it.pagopa.pn.delivery.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnMandateEvent;

import java.util.function.Consumer;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.svc.NotificationDelegatedService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ContextConfiguration(classes = {MandateHandler.class})
@ExtendWith(SpringExtension.class)
class MandateHandlerTest {

    @Autowired
    private MandateHandler mandateHandler;

    @MockBean
    private NotificationDelegatedService notificationDelegatedService;

    /**
     * Method under test: {@link MandateHandler#pnDeliveryAcceptedMandateConsumer()}
     */
    @Test
    void testPnDeliveryAcceptedMandateConsumer() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doNothing().when(notificationDelegatedService).handleAcceptedMandate(same(payload), eq(EventType.MANDATE_ACCEPTED));

        Consumer<Message<PnMandateEvent.Payload>> consumer = mandateHandler.pnDeliveryAcceptedMandateConsumer();
        assertDoesNotThrow(() -> consumer.accept(new GenericMessage<>(payload)));
    }

    /**
     * Method under test: {@link MandateHandler#pnDeliveryAcceptedMandateConsumer()}
     */
    @Test
    void testPnDeliveryAcceptedMandateConsumerException() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doThrow(PnInternalException.class).when(notificationDelegatedService).handleAcceptedMandate(any(), any());

        Consumer<Message<PnMandateEvent.Payload>> consumer = mandateHandler.pnDeliveryAcceptedMandateConsumer();
        Message<PnMandateEvent.Payload> message = new GenericMessage<>(payload);
        assertThrows(PnInternalException.class, () -> consumer.accept(message));
    }

    @Test
    void testPnDeliveryUpdatedMandateConsumer() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doNothing().when(notificationDelegatedService).handleUpdatedMandate(same(payload), eq(EventType.MANDATE_UPDATED));

        Consumer<Message<PnMandateEvent.Payload>> consumer = mandateHandler.pnDeliveryUpdatedMandateConsumer();
        assertDoesNotThrow(() -> consumer.accept(new GenericMessage<>(payload)));
    }

    @Test
    void testPnDeliveryUpdatedMandateConsumerException() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doThrow(PnInternalException.class).when(notificationDelegatedService).handleUpdatedMandate(any(), any());

        Consumer<Message<PnMandateEvent.Payload>> consumer = mandateHandler.pnDeliveryUpdatedMandateConsumer();
        Message<PnMandateEvent.Payload> message = new GenericMessage<>(payload);
        assertThrows(PnInternalException.class, () -> consumer.accept(message));
    }


    /**
     * Method under test: {@link MandateHandler#pnDeliveryAcceptedMandateConsumer()}
     */
    @Test
    void testPnDeliveryRejectedMandateConsumer() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doNothing().when(notificationDelegatedService).handleAcceptedMandate(same(payload), eq(EventType.MANDATE_REJECTED));

        Consumer<Message<PnMandateEvent.Payload>> consumer = mandateHandler.pnDeliveryRejectedMandateConsumer();
        assertDoesNotThrow(() -> consumer.accept(new GenericMessage<>(payload)));
    }

    /**
     * Method under test: {@link MandateHandler#pnDeliveryAcceptedMandateConsumer()}
     */
    @Test
    void testPnDeliveryRejectedMandateConsumerException() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doThrow(PnInternalException.class).when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(any(), any());

        Consumer<Message<PnMandateEvent.Payload>> consumer = mandateHandler.pnDeliveryRejectedMandateConsumer();
        Message<PnMandateEvent.Payload> message = new GenericMessage<>(payload);
        assertThrows(PnInternalException.class, () -> consumer.accept(message));
    }

    /**
     * Method under test: {@link MandateHandler#pnDeliveryAcceptedMandateConsumer()}
     */
    @Test
    void testPnDeliveryRevokedMandateConsumer() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doNothing().when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(same(payload.getMandateId()), eq(EventType.MANDATE_REVOKED));

        Consumer<Message<PnMandateEvent.Payload>> consumer = mandateHandler.pnDeliveryRevokedMandateConsumer();
        assertDoesNotThrow(() -> consumer.accept(new GenericMessage<>(payload)));
    }

    /**
     * Method under test: {@link MandateHandler#pnDeliveryAcceptedMandateConsumer()}
     */
    @Test
    void testPnDeliveryRevokedMandateConsumerException() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doThrow(PnInternalException.class).when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(any(), any());

        Consumer<Message<PnMandateEvent.Payload>> consumer = mandateHandler.pnDeliveryRevokedMandateConsumer();
        Message<PnMandateEvent.Payload> message = new GenericMessage<>(payload);
        assertThrows(PnInternalException.class, () -> consumer.accept(message));
    }

    /**
     * Method under test: {@link MandateHandler#pnDeliveryAcceptedMandateConsumer()}
     */
    @Test
    void testPnDeliveryExpiredMandateConsumer() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doNothing().when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(same(payload.getMandateId()), eq(EventType.MANDATE_EXPIRED));

        Consumer<Message<PnMandateEvent.Payload>> consumer = mandateHandler.pnDeliveryExpiredMandateConsumer();
        assertDoesNotThrow(() -> consumer.accept(new GenericMessage<>(payload)));
    }

    /**
     * Method under test: {@link MandateHandler#pnDeliveryAcceptedMandateConsumer()}
     */
    @Test
    void testPnDeliveryExpiredMandateConsumerException() {
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        doThrow(PnInternalException.class).when(notificationDelegatedService).deleteNotificationDelegatedByMandateId(any(), any());

        Consumer<Message<PnMandateEvent.Payload>> consumer = mandateHandler.pnDeliveryExpiredMandateConsumer();
        Message<PnMandateEvent.Payload> message = new GenericMessage<>(payload);
        assertThrows(PnInternalException.class, () -> consumer.accept(message));
    }
}

