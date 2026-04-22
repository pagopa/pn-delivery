package it.pagopa.pn.delivery.svc.validation.context;

import it.pagopa.pn.delivery.models.InternalNotification;
import lombok.Data;

import java.util.List;

@Data
public class LegalNotificationContext implements NotificationContext {
    InternalNotification payload;
    String cxId;
    List<String> cxGroups;
}
