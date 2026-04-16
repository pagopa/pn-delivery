package it.pagopa.pn.delivery.svc.validation.context;

import it.pagopa.pn.delivery.models.InternalNotification;

public interface NotificationContext extends ValidationContext<InternalNotification> {
    String getCxId();
}
