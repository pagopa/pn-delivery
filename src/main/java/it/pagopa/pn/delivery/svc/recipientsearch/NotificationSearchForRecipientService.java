package it.pagopa.pn.delivery.svc.recipientsearch;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationSearchForRecipientService {

    private final NotificationDao notificationDao;

    public NotificationSearchForRecipientService(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    public List<NotificationSearchRow> searchReceivedNotification(
            String recipientId, Instant startDate, Instant endDate,
            String senderId, NotificationStatus status, String subjectRegExp
    ) {
        return notificationDao.searchReceivedNotification(recipientId, startDate, endDate, senderId, status, subjectRegExp);
    }
}
