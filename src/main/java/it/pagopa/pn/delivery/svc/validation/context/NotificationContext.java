package it.pagopa.pn.delivery.svc.validation.context;

import it.pagopa.pn.delivery.models.InternalNotification;

import java.util.List;

public interface NotificationContext extends ValidationContext<InternalNotification> {
    String getCxId();
    List<String> getCxGroups();
}
