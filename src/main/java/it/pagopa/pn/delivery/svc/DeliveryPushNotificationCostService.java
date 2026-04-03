package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.models.NotificationCostRequest;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;

public class DeliveryPushNotificationCostService implements NotificationCostService {

    private final PnDeliveryPushClientImpl pnDeliveryPushClient;
    private final NotificationProcessCostResponseMapper notificationMapper;

    public DeliveryPushNotificationCostService(PnDeliveryPushClientImpl pnDeliveryPushClient, NotificationProcessCostResponseMapper notificationMapper) {
        this.pnDeliveryPushClient = pnDeliveryPushClient;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public NotificationProcessCostResponseInt getNotificationCost(NotificationCostRequest request) {
        return notificationMapper.fromExternal(pnDeliveryPushClient.getNotificationProcessCost(request.iun(), request.recipientIdx(), request.notificationFeePolicy(), request.applyCost(), request.paFee(), request.vat()));
    }
}
