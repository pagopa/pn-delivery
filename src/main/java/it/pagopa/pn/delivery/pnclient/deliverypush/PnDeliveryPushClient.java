package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineStatusHistoryDto;

import java.time.Instant;
import java.util.Set;

public interface PnDeliveryPushClient {
    TimelineStatusHistoryDto getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt);
}
