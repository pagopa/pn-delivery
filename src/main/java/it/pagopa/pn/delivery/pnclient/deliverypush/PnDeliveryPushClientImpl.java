package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.NotificationProcessCostApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.TimelineAndStatusApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@CustomLog
@Component
@RequiredArgsConstructor
public class PnDeliveryPushClientImpl {

    private final TimelineAndStatusApi timelineAndStatusApi;
    private final NotificationProcessCostApi notificationProcessCostApi;


    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, OffsetDateTime createdAt) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY_PUSH, "getTimelineAndStatusHistory");
        return timelineAndStatusApi.getNotificationHistory( iun, numberOfRecipients, createdAt);
    }

    public NotificationProcessCostResponse getNotificationProcessCost(String iun, int recipientIdx, NotificationFeePolicy notificationFeePolicy, boolean applyCost, Integer paFee, Integer vat){
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY_PUSH, "getNotificationProcessCost");
        return notificationProcessCostApi.notificationProcessCost( iun, recipientIdx, notificationFeePolicy, applyCost, paFee, vat );
    }

}
