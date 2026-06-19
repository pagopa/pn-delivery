package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.NotificationDetail;

import java.util.List;

public interface NotificationDetailRetrieverStrategy<T extends NotificationDetail> {
    T getNotificationInformation(String iun, boolean withTimeline, boolean requestBySender, String senderId);

    T getNotificationInformationWithSenderIdCheck(String iun, String senderId, List<String> groups);

    T getNotificationInformation(String senderId, String paProtocolNumber, String idempotenceToken, List<String> groups);

    T getNotificationAndNotifyViewedEvent(String iun, InternalAuthHeader internalAuthHeader, String mandateId, PnAuditLogEvent logEvent);

}
