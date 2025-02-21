package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.middleware.notificationdao.PaNotificationLimitDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.NotificationRefusedPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaNotificationLimitServiceTest {

    @Mock
    private PaNotificationLimitDao paNotificationLimitDao;

    @InjectMocks
    private PaNotificationLimitService paNotificationLimitService;

    @Mock
    private NotificationRefusedVerificationService notificationRefusedVerificationService;

    @Test
    void decrementLimitIncrementDailyCounter_success() {
        InternalNotification notification = getInternalNotification();
        when(paNotificationLimitDao.decrementLimitIncrementDailyCounter(anyString(), any(OffsetDateTime.class))).thenReturn(true);

        paNotificationLimitService.decrementLimitIncrementDailyCounter(notification);

        verify(paNotificationLimitDao).decrementLimitIncrementDailyCounter(notification.getSenderPaId(), notification.getSentAt());
    }

    @Test
    void decrementLimitIncrementDailyCounter_limitExceeded() {
        InternalNotification notification = getInternalNotification();
        when(paNotificationLimitDao.decrementLimitIncrementDailyCounter(anyString(), any(OffsetDateTime.class))).thenReturn(false);

        assertThrows(PnBadRequestException.class, () -> paNotificationLimitService.decrementLimitIncrementDailyCounter(notification));
    }

    @Test
    void incrementLimitDecrementDailyCounter_success() {
        InternalNotification notification = getInternalNotification();

        paNotificationLimitService.incrementLimitDecrementDailyCounter(notification);

        verify(paNotificationLimitDao).incrementLimitDecrementDailyCounter(notification.getSenderPaId(), notification.getSentAt());
    }

    @Test
    void checkIfPaNotificationLimitExists_itemExists() {
        InternalNotification notification = getInternalNotification();
        when(paNotificationLimitDao.checkIfPaNotificationLimitExists(notification.getSenderPaId(), notification.getSentAt())).thenReturn(true);

        boolean result = paNotificationLimitService.checkIfPaNotificationLimitExists(notification);

        assertTrue(result);
        verify(paNotificationLimitDao).checkIfPaNotificationLimitExists(notification.getSenderPaId(), notification.getSentAt());
    }

    @Test
    void handleNotificationRefused_paNotificationLimitExistsAndVerificationSuccess() {
        NotificationRefusedPayload payload = getNotificationRefusedPayload();
        when(paNotificationLimitDao.checkIfPaNotificationLimitExists(payload.getPaId(), OffsetDateTime.parse(payload.getSentAt()))).thenReturn(true);
        when(notificationRefusedVerificationService.putNotificationRefusedVerification(payload.getTimelineId())).thenReturn(true);

        paNotificationLimitService.handleNotificationRefused(payload);

        verify(paNotificationLimitDao).incrementLimitDecrementDailyCounter(payload.getPaId(), OffsetDateTime.parse(payload.getSentAt()));
    }

    @Test
    void handleNotificationRefused_paNotificationLimitExistsAndVerificationFails() {
        NotificationRefusedPayload payload = getNotificationRefusedPayload();
        when(paNotificationLimitDao.checkIfPaNotificationLimitExists(payload.getPaId(), OffsetDateTime.parse(payload.getSentAt()))).thenReturn(true);
        when(notificationRefusedVerificationService.putNotificationRefusedVerification(payload.getTimelineId())).thenReturn(false);

        paNotificationLimitService.handleNotificationRefused(payload);

        verify(paNotificationLimitDao, never()).incrementLimitDecrementDailyCounter(payload.getPaId(), OffsetDateTime.parse(payload.getSentAt()));
    }

    @Test
    void handleNotificationRefused_paNotificationLimitDoesNotExist() {
        NotificationRefusedPayload payload = getNotificationRefusedPayload();
        when(paNotificationLimitDao.checkIfPaNotificationLimitExists(payload.getPaId(), OffsetDateTime.parse(payload.getSentAt()))).thenReturn(false);

        paNotificationLimitService.handleNotificationRefused(payload);

        verify(notificationRefusedVerificationService, never()).putNotificationRefusedVerification(payload.getTimelineId());
        verify(paNotificationLimitDao, never()).incrementLimitDecrementDailyCounter(payload.getPaId(), OffsetDateTime.parse(payload.getSentAt()));
    }

    @Test
    void handleNotificationRefused_exceptionThrown() {
        NotificationRefusedPayload payload = getNotificationRefusedPayload();
        when(paNotificationLimitDao.checkIfPaNotificationLimitExists(payload.getPaId(), OffsetDateTime.parse(payload.getSentAt()))).thenThrow(new RuntimeException("Database error"));

        assertThrows(PnInternalException.class, () -> paNotificationLimitService.handleNotificationRefused(payload));
    }

    private NotificationRefusedPayload getNotificationRefusedPayload() {
        NotificationRefusedPayload notificationRefusedPayload = new NotificationRefusedPayload();
        notificationRefusedPayload.setIun("testIun");
        notificationRefusedPayload.setPaId("testPaId");
        notificationRefusedPayload.setSentAt(OffsetDateTime.now().toString());
        notificationRefusedPayload.setTimelineId("testTimelineId");
        return notificationRefusedPayload;
    }

    private InternalNotification getInternalNotification() {
        return InternalNotification.builder().
                senderPaId("testPaId").
                sentAt(OffsetDateTime.now()).
                build();
    }
}
