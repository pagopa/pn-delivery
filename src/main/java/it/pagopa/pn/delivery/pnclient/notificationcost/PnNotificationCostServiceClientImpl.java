package it.pagopa.pn.delivery.pnclient.notificationcost;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.api.NotificationCostRecipientApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.NotificationCostPaymentResponse;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class PnNotificationCostServiceClientImpl {

    private final NotificationCostRecipientApi notificationCostRecipientApi;
    private final NotificationCostRecipientApi monitoringNotificationCostRecipientApi;

    public PnNotificationCostServiceClientImpl(
            NotificationCostRecipientApi notificationCostRecipientApi,
            @Qualifier("monitoringNotificationCostRecipientApi") NotificationCostRecipientApi monitoringNotificationCostRecipientApi
    ) {
        this.notificationCostRecipientApi = notificationCostRecipientApi;
        this.monitoringNotificationCostRecipientApi = monitoringNotificationCostRecipientApi;
    }

    public NotificationCostPaymentResponse getNotificationCostByPayment(String paTaxId, String noticeCode) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_NOTIFICATION_COST_SERVICE, "getNotificationCost");
        return notificationCostRecipientApi.getNotificationCostByPayment(paTaxId, noticeCode);
    }

    public NotificationCostPaymentResponse getNotificationCostByPaymentForMonitoring(String paTaxId, String noticeCode) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_NOTIFICATION_COST_SERVICE, "getNotificationCostByPaymentForMonitoring");
        return monitoringNotificationCostRecipientApi.getNotificationCostByPayment(paTaxId, noticeCode);
    }
}
