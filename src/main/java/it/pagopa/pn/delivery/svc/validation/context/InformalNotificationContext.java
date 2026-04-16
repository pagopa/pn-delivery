package it.pagopa.pn.delivery.svc.validation.context;

import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.campaign.Campaign;
import lombok.Data;

import java.util.List;

@Data
public class InformalNotificationContext implements NotificationContext {
    InternalNotification payload;
    String cxId;
    String messageId;
    Campaign campaign;
    List<String> additionalLanguages;
    String contentType;
}
