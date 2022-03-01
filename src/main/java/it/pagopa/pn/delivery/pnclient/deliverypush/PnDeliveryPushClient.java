package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;

import java.util.Set;

public interface PnDeliveryPushClient {
    Set<TimelineElement> getTimelineElements(String iun);
}
