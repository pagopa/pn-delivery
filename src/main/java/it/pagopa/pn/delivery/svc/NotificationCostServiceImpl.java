package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationCancelledException;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.NotificationCostPaymentResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.timelineservice.v1.model.DeliveryInformationResponse;
import it.pagopa.pn.delivery.models.NotificationCostRequest;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;
import it.pagopa.pn.delivery.pnclient.notificationcost.PnNotificationCostServiceClientImpl;
import it.pagopa.pn.delivery.pnclient.timelineservice.PnTimelineServiceClientImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.delivery.svc.NotificationPriceService.ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCostServiceImpl implements NotificationCostService {

    private final PnTimelineServiceClientImpl pnTimelineServiceClient;
    private final PnNotificationCostServiceClientImpl pnNotificationCostServiceClient;
    private final NotificationProcessCostResponseMapper notificationMapper;

    @Override
    public NotificationProcessCostResponseInt getNotificationCost(NotificationCostRequest request) {
        log.info("getNotificationCost - IUN={} recIndex={}", request.iun(), request.recipientIdx());
        DeliveryInformationResponse deliveryInformation = pnTimelineServiceClient.getDeliveryInformation(request.iun(), request.recipientIdx());

        if (deliveryInformation.getIsNotificationCancelled()) {
            throw new PnNotificationCancelledException("Cannot retrieve price for cancelled notification");
        }
        if (!deliveryInformation.getIsNotificationAccepted()){
            throw new PnNotFoundException("Notification is not ACCEPTED", String.format(
                    "Notification with iun=%s, has not been accepted yet", request.iun()),
                    ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED);
        }

        NotificationCostPaymentResponse notificationCostByPayment = pnNotificationCostServiceClient.getNotificationCostByPayment(request.iuv());
        return notificationMapper.mapFromTimelineAndCostResponse(deliveryInformation, notificationCostByPayment);
    }
}
