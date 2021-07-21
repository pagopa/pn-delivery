package it.pagopa.pn.delivery.model.notification.timeline;

import it.pagopa.pn.delivery.model.notification.NotificationAttachment;
import it.pagopa.pn.delivery.model.notification.NotificationRecipient;

import java.util.List;

public class ReceivedDetails implements TimelineElementDetails {

    private NotificationAttachment.Digests digests;
    private List<NotificationRecipient> recipients;

    public NotificationAttachment.Digests getDigests() {
        return digests;
    }

    public void setDigests(NotificationAttachment.Digests digests) {
        this.digests = digests;
    }

    public List<NotificationRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<NotificationRecipient> recipients) {
        this.recipients = recipients;
    }
}
