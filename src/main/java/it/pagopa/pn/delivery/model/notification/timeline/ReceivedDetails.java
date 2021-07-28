package it.pagopa.pn.delivery.model.notification.timeline;

import it.pagopa.pn.delivery.model.notification.NotificationAttachmentDigests;
import it.pagopa.pn.delivery.model.notification.NotificationRecipient;

import java.util.List;

public class ReceivedDetails implements TimelineElementDetails {

    private NotificationAttachmentDigests digests;
    private List<NotificationRecipient> recipients;

    public NotificationAttachmentDigests getDigests() {
        return digests;
    }

    public void setDigests(NotificationAttachmentDigests digests) {
        this.digests = digests;
    }

    public List<NotificationRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<NotificationRecipient> recipients) {
        this.recipients = recipients;
    }
}
