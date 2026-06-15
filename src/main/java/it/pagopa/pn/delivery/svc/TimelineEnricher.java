package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.models.NotificationDetail;

public interface TimelineEnricher<T extends NotificationDetail> {
    void enrichNotificationDetail(T notificationDetail, boolean requestBySender);
}
