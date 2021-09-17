package it.pagopa.pn.delivery.svc.sendersearch;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationSearchForSenderService {

    private final NotificationDao notificationDao;

    public NotificationSearchForSenderService(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    public List<NotificationSearchRow> searchSentNotification(
            String senderId, Instant startDate, Instant endDate,
            String recipientId, NotificationStatus status, String subjectRegExp
    ) {
        return notificationDao.searchSentNotification(senderId, startDate, endDate, recipientId, status, subjectRegExp);
    }
}
