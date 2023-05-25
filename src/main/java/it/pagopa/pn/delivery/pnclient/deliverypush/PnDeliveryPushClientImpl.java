package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.NotificationProcessCostApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.TimelineAndStatusApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PnDeliveryPushClientImpl {

    private final TimelineAndStatusApi timelineAndStatusApi;
    private final NotificationProcessCostApi notificationProcessCostApi;


    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, OffsetDateTime createdAt) {
        return timelineAndStatusApi.getNotificationHistory( iun, numberOfRecipients, createdAt);
    }

    public NotificationProcessCostResponse getNotificationProcessCost(String iun, int recipientIdx, NotificationFeePolicy notificationFeePolicy){
        return notificationProcessCostApi.notificationProcessCost( iun, recipientIdx, notificationFeePolicy );
    }

}
