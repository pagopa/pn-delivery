package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import it.pagopa.pn.delivery.models.NotificationCostRequest;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryPushNotificationCostService implements NotificationCostService {

    private final PnDeliveryPushClientImpl pnDeliveryPushClient;
    private final NotificationProcessCostResponseMapper notificationMapper;

    @Override
    public NotificationProcessCostResponseInt getNotificationCost(NotificationCostRequest request) {
        log.info("getNotificationCost - IUN={} recIndex={}", request.iun(), request.recipientIdx());
        NotificationProcessCostResponse response = pnDeliveryPushClient.getNotificationProcessCost(
                request.iun(),
                request.recipientIdx(),
                request.notificationFeePolicy(),
                request.applyCost(),
                request.paFee(),
                request.vat()
        );
        return notificationMapper.fromExternal(response);
    }
}
