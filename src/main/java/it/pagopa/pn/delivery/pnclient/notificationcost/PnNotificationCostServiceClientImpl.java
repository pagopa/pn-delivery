package it.pagopa.pn.delivery.pnclient.notificationcost;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.api.NotificationCostRecipientApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.NotificationCostPaymentResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
public class PnNotificationCostServiceClientImpl {

    private final NotificationCostRecipientApi notificationCostRecipientApi;

    public NotificationCostPaymentResponse getNotificationCostByPayment(String iuv) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_NOTIFICATION_COST_SERVICE, "getNotificationCost");
        return notificationCostRecipientApi.getNotificationCostByPayment(iuv);
    }
}
