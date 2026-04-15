package it.pagopa.pn.delivery.svc.validation.context;

import it.pagopa.pn.delivery.models.InternalNotification;
import lombok.Data;

@Data
public class NotificaLegaleContext implements NotificaContext {
    InternalNotification payload;
    String cxId;
}
