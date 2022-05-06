package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.api.dto.notification.timeline.NotificationHistoryResponse;
import java.time.Instant;

public interface PnDeliveryPushClient {
    NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt);
}
