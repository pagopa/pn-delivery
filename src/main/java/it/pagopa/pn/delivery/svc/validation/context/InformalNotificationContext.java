package it.pagopa.pn.delivery.svc.validation.context;

import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.campaign.Campaign;
import lombok.Data;

@Data
public class InformalNotificationContext implements NotificationContext {
    InternalNotification payload;
    String cxId;
    Campaign campaign;
}
