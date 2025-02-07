package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.middleware.notificationdao.PaNotificationLimitDao;
import it.pagopa.pn.delivery.models.InternalNotification;
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

    private InternalNotification getInternalNotification() {
        return InternalNotification.builder().
                senderPaId("testPaId").
                sentAt(OffsetDateTime.now()).
                build();
    }
}
