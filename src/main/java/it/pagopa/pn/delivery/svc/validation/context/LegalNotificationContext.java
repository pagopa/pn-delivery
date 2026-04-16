package it.pagopa.pn.delivery.svc.validation.context;

import it.pagopa.pn.delivery.models.InternalNotification;
import lombok.Data;

@Data
public class LegalNotificationContext implements NotificationContext {
    InternalNotification payload;
    String cxId;
}
