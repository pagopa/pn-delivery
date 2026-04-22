package it.pagopa.pn.delivery.svc.validation.context;

import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.campaign.Campaign;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class InformalNotificationContext implements NotificationContext {
    InternalNotification payload;
    String cxId;
    List<String> cxGroups;
    Campaign campaign;
}
