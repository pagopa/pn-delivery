package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.middleware.notificationdao.PaNotificationLimitDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Service
@CustomLog
public class PaNotificationLimitService {

    private final PaNotificationLimitDao paNotificationLimitDao;

    @Autowired
    public PaNotificationLimitService(PaNotificationLimitDao paNotificationLimitDao) {
        this.paNotificationLimitDao = paNotificationLimitDao;
    }

    public void decrementLimitIncrementDailyCounter(InternalNotification internalNotification) throws ConditionalCheckFailedException {
        boolean updated = paNotificationLimitDao.decrementLimitIncrementDailyCounter(createPrimaryKey(internalNotification), createDailyCounter(internalNotification));
        if(! updated){
            throw Exception for 429;
        }
    }

    public void incrementLimitDecrementDailyCounter(InternalNotification internalNotification) {
        paNotificationLimitDao.incrementLimitDecrementDailyCounter(createPrimaryKey(internalNotification), createDailyCounter(internalNotification));
    }

    public boolean checkIfPaNotificationLimitExists(InternalNotification internalNotification) {
        return paNotificationLimitDao.checkIfPaNotificationLimitExists(createPrimaryKey(internalNotification));
    }

    private String createPrimaryKey(InternalNotification internalNotification) { //todo prevedere eventuali nullPointer?
        return internalNotification.getSenderPaId() + "#" + internalNotification.getSentAt().getYear() + "#" + internalNotification.getSentAt().getMonthValue();
    }

    private String createDailyCounter(InternalNotification internalNotification) {
        return "dailyCounter" + String.format("%02d", internalNotification.getSentAt().getDayOfMonth());
    }
}
