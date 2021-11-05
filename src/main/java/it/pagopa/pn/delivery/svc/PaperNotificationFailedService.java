package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.commons_delivery.middleware.failednotification.PaperNotificationFailedDao;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PaperNotificationFailedService {
    private PaperNotificationFailedDao paperNotificationFailedDao;

    public PaperNotificationFailedService(PaperNotificationFailedDao paperNotificationFailedDao) {
        this.paperNotificationFailedDao = paperNotificationFailedDao;
    }

    public List<PaperNotificationFailed> getPaperNotificationsFailed(String recipientId) {
        return new ArrayList<>(paperNotificationFailedDao.getNotificationByRecipientId(recipientId));
    }

}
