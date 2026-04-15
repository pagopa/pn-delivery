package it.pagopa.pn.delivery.svc.validation.context;

import it.pagopa.pn.delivery.models.InternalNotification;

public interface ValidationContext {
    InternalNotification getPayload();
    String getCxId();
}