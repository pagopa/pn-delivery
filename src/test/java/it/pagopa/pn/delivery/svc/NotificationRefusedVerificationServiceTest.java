package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.middleware.notificationdao.NotificationRefusedVerificationDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRefusedVerificationServiceTest {

    @Mock
    private NotificationRefusedVerificationDao notificationRefusedVerificationDao;

    @InjectMocks
    private NotificationRefusedVerificationService notificationRefusedVerificationService;

    @Test
    void putNotificationRefusedVerification_success() {
        String pk = "testPk";
        when(notificationRefusedVerificationDao.putNotificationRefusedVerification(pk)).thenReturn(true);

        boolean result = notificationRefusedVerificationService.putNotificationRefusedVerification(pk);

        assertTrue(result);
    }

    @Test
    void putNotificationRefusedVerification_failure() {
        String pk = "testPk";
        when(notificationRefusedVerificationDao.putNotificationRefusedVerification(pk)).thenReturn(false);

        boolean result = notificationRefusedVerificationService.putNotificationRefusedVerification(pk);

        assertFalse(result);
    }

}
