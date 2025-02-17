package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.middleware.notificationdao.NotificationRefusedVerificationDao;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class NotificationRefusedVerificationService {

    private final NotificationRefusedVerificationDao notificationRefusedVerificationDao;

    @Autowired
    public NotificationRefusedVerificationService(NotificationRefusedVerificationDao notificationRefusedVerificationDao) {
        this.notificationRefusedVerificationDao = notificationRefusedVerificationDao;
    }

    public boolean putNotificationRefusedVerification(String pk) {
        return notificationRefusedVerificationDao.putNotificationRefusedVerification(pk);
    }
}
