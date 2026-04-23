package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationCancelledException;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.NotificationProcessCostApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.TimelineAndStatusApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

import static it.pagopa.pn.delivery.svc.NotificationPriceService.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED;
import static it.pagopa.pn.delivery.svc.NotificationPriceService.ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED;

@CustomLog
@Component
@RequiredArgsConstructor
public class PnDeliveryPushClientImpl {

    private final TimelineAndStatusApi timelineAndStatusApi;
    private final NotificationProcessCostApi notificationProcessCostApi;


    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, OffsetDateTime createdAt) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY_PUSH, "getTimelineAndStatusHistory");
        return timelineAndStatusApi.getNotificationHistory(iun, numberOfRecipients, createdAt);
    }

    public NotificationProcessCostResponse getNotificationProcessCost(String iun, int recipientIdx, NotificationFeePolicy notificationFeePolicy, boolean applyCost, Integer paFee, Integer vat) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY_PUSH, "getNotificationProcessCost");
        try {
            return notificationProcessCostApi.notificationProcessCost(iun, recipientIdx, notificationFeePolicy, applyCost, paFee, vat);
        } catch (Exception exc) {
            // nel caso in cui la risposta da parte di delivery push è un 404, devo controllare che la causale
            // sia per colpa della notifica cancellata. Se si, ritorno a mia volta un 404, altrimenti ritorno
            // direttamente l'exception originale
            if (exc instanceof PnHttpResponseException pnHttpResponseException
                    && pnHttpResponseException.getStatusCode() == HttpStatus.NOT_FOUND.value()
                    && (pnHttpResponseException.getProblem().getErrors().get(0).getCode().equals(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED))) {

                throw new PnNotificationCancelledException("Cannot retrieve price for cancelled notification", exc);
            }
            if (exc instanceof PnHttpResponseException pnHttpResponseException
                    && pnHttpResponseException.getStatusCode() == HttpStatus.NOT_FOUND.value()
                    && (pnHttpResponseException.getProblem().getErrors().get(0).getCode().equals(ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED))) {
                throw new PnNotFoundException("Notification is not ACCEPTED", String.format(
                        "Notification with iun=%s, has not been accepted yet", iun),
                        ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED);
            }
            throw exc;
        }
    }

}
