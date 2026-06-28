package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.models.NotificationDetail;

import java.util.List;

public interface NotificationDetailRetrieverStrategy<T extends NotificationDetail> {
    T getNotificationInformationWithSenderIdCheck(String iun, String senderId, List<String> groups);

    T getNotificationInformation(String senderId, String paProtocolNumber, String idempotenceToken, List<String> groups);
}
