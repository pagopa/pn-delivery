package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.middleware.notificationdao.PaNotificationLimitDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATION_LIMIT_EXCEEDED;

@Service
@CustomLog
public class PaNotificationLimitService {

    private final PaNotificationLimitDao paNotificationLimitDao;

    @Autowired
    public PaNotificationLimitService(PaNotificationLimitDao paNotificationLimitDao) {
        this.paNotificationLimitDao = paNotificationLimitDao;
    }

    public void decrementLimitIncrementDailyCounter(InternalNotification internalNotification) {
        boolean updated = paNotificationLimitDao.decrementLimitIncrementDailyCounter(internalNotification.getSenderPaId(), internalNotification.getSentAt());
        if (!updated) {
            throw new PnBadRequestException("NOTIFICATION_LIMIT_EXCEEDED", "Notification limit exceeded for paId: " + internalNotification.getSenderPaId(), ERROR_CODE_DELIVERY_NOTIFICATION_LIMIT_EXCEEDED);
        }
    }

    public void incrementLimitDecrementDailyCounter(InternalNotification internalNotification) {
        paNotificationLimitDao.incrementLimitDecrementDailyCounter(internalNotification.getSenderPaId(), internalNotification.getSentAt());
    }

    public boolean checkIfPaNotificationLimitExists(InternalNotification internalNotification) {
        return paNotificationLimitDao.checkIfPaNotificationLimitExists(internalNotification.getSenderPaId(), internalNotification.getSentAt());
    }
}
