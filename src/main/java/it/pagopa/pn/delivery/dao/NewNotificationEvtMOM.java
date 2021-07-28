package it.pagopa.pn.delivery.dao;

import it.pagopa.pn.commons.mom.MomConsumer;
import it.pagopa.pn.commons.mom.MomProducer;
import it.pagopa.pn.delivery.model.events.NewNotificationEvt;

public interface NewNotificationEvtMOM extends MomProducer<NewNotificationEvt>, MomConsumer<NewNotificationEvt> {
}
