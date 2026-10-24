package it.pagopa.pn.delivery.middleware.queue.consumer;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import it.pagopa.pn.delivery.models.NotificationRefusedPayload;
import it.pagopa.pn.delivery.svc.PaNotificationLimitService;
import org.springframework.messaging.MessageHeaders;

class NotificationRefusedConsumerTest {

    private PaNotificationLimitService paNotificationLimitService;
    private NotificationRefusedConsumer notificationRefusedConsumer;

    @BeforeEach
    void setUp() {
        paNotificationLimitService = mock(PaNotificationLimitService.class);
        notificationRefusedConsumer = new NotificationRefusedConsumer(paNotificationLimitService);
    }

    @Test
    void pnDeliveryNotificationRefusedConsumer_processesMessageSuccessfully() {
        Message<NotificationRefusedPayload> message = mock(Message.class);
        MessageHeaders headers = mock(MessageHeaders.class);
        when(message.getHeaders()).thenReturn(headers);
        NotificationRefusedPayload payload = new NotificationRefusedPayload();
        when(message.getPayload()).thenReturn(payload);

        notificationRefusedConsumer.consume(message);

        verify(paNotificationLimitService, times(1)).handleNotificationRefused(payload);
    }

    @Test
    void pnDeliveryNotificationRefusedConsumer_logsAndThrowsExceptionOnError() {
        Message<NotificationRefusedPayload> message = mock(Message.class);
        MessageHeaders headers = mock(MessageHeaders.class);
        when(message.getHeaders()).thenReturn(headers);
        NotificationRefusedPayload payload = new NotificationRefusedPayload();
        when(message.getPayload()).thenReturn(payload);
        doThrow(new RuntimeException("Test Exception")).when(paNotificationLimitService).handleNotificationRefused(payload);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationRefusedConsumer.consume(message);
        });

        assertEquals("Test Exception", exception.getMessage());
        verify(paNotificationLimitService, times(1)).handleNotificationRefused(payload);
    }
}
