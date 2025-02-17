package it.pagopa.pn.delivery.middleware.queue.consumer.handler;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import it.pagopa.pn.delivery.models.NotificationRefusedPayload;
import it.pagopa.pn.delivery.svc.PaNotificationLimitService;

class NotificationRefusedHandlerTest {

    private PaNotificationLimitService paNotificationLimitService;
    private NotificationRefusedHandler notificationRefusedHandler;

    @BeforeEach
    void setUp() {
        paNotificationLimitService = mock(PaNotificationLimitService.class);
        notificationRefusedHandler = new NotificationRefusedHandler(paNotificationLimitService);
    }

    @Test
    void pnDeliveryNotificationRefusedConsumer_processesMessageSuccessfully() {
        Message<NotificationRefusedPayload> message = mock(Message.class);
        NotificationRefusedPayload payload = new NotificationRefusedPayload();
        when(message.getPayload()).thenReturn(payload);

        notificationRefusedHandler.pnDeliveryNotificationRefusedConsumer().accept(message);

        verify(paNotificationLimitService, times(1)).handleNotificationRefused(payload);
    }

    @Test
    void pnDeliveryNotificationRefusedConsumer_logsAndThrowsExceptionOnError() {
        Message<NotificationRefusedPayload> message = mock(Message.class);
        NotificationRefusedPayload payload = new NotificationRefusedPayload();
        when(message.getPayload()).thenReturn(payload);
        doThrow(new RuntimeException("Test Exception")).when(paNotificationLimitService).handleNotificationRefused(payload);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationRefusedHandler.pnDeliveryNotificationRefusedConsumer().accept(message);
        });

        assertEquals("Test Exception", exception.getMessage());
        verify(paNotificationLimitService, times(1)).handleNotificationRefused(payload);
    }
}
