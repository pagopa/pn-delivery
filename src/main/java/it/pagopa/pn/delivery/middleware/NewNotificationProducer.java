package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.api.dto.events.NewNotificationEvent;
import it.pagopa.pn.commons.abstractions.MomProducer;

public interface NewNotificationProducer extends MomProducer<NewNotificationEvent> {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-producer";

}
