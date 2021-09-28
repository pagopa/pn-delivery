package it.pagopa.pn.delivery.svc.searchnotification;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationSearchService {

    private final NotificationDao notificationDao;

    public NotificationSearchService(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    public List<NotificationSearchRow> searchNotification(
            boolean bySender, String senderId, Instant startDate, Instant endDate,
            String recipientId, NotificationStatus status, String subjectRegExp
    ) {
        return notificationDao.searchNotification(bySender, senderId, startDate, endDate, recipientId, status, subjectRegExp);
    }
}
