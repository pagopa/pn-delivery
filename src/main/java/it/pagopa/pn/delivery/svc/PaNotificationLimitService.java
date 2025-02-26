package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.middleware.notificationdao.PaNotificationLimitDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.NotificationRefusedPayload;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_HANDLEEVENTFAILED;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATION_LIMIT_EXCEEDED;

@Service
@CustomLog
public class PaNotificationLimitService {

    private final PaNotificationLimitDao paNotificationLimitDao;
    private final NotificationRefusedVerificationService notificationRefusedVerificationService;

    @Autowired
    public PaNotificationLimitService(PaNotificationLimitDao paNotificationLimitDao, NotificationRefusedVerificationService notificationRefusedVerificationService) {
        this.paNotificationLimitDao = paNotificationLimitDao;
        this.notificationRefusedVerificationService = notificationRefusedVerificationService;
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

    public void handleNotificationRefused(NotificationRefusedPayload notificationRefusedPayload) {
        log.info("Handling notification refused for {}", notificationRefusedPayload);
        try {
            if (!paNotificationLimitDao.checkIfPaNotificationLimitExists(notificationRefusedPayload.getPaId(), OffsetDateTime.parse(notificationRefusedPayload.getSentAt()))) {
                log.info("Notification refused for iun={} not handled. No limit found for paId={} on PaNotificationLimit: nothing to update", notificationRefusedPayload.getIun(), notificationRefusedPayload.getPaId());
                return;
            }
            if (notificationRefusedVerificationService.putNotificationRefusedVerification(notificationRefusedPayload.getTimelineId())) {
                paNotificationLimitDao.incrementLimitDecrementDailyCounter(notificationRefusedPayload.getPaId(), OffsetDateTime.parse(notificationRefusedPayload.getSentAt()));
            }
            log.info("Successfully handled notification refused for iun={}", notificationRefusedPayload.getIun());
        } catch (Exception e) {
            log.error("Error handling notification refused for {}", notificationRefusedPayload, e);
            throw new PnInternalException("Error handling notification refused", ERROR_CODE_DELIVERY_HANDLEEVENTFAILED);
        }
    }
}
