package it.pagopa.pn.delivery.pnclient.notificationcost;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.api.NotificationCostRecipientApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.NotificationCostRecipientResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
public class PnNotificationCostServiceClientImpl {

    private final NotificationCostRecipientApi notificationCostRecipientApi;

    public NotificationCostRecipientResponse getNotificationCostRecipient(String iun, Integer recIndex) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_NOTIFICATION_COST_SERVICE, "getNotificationCost");
        return notificationCostRecipientApi.getNotificationCost(iun, recIndex);
    }
}
