package it.pagopa.pn.delivery;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class NewNotificationValidator {

    public Notification checkNewNotificationBeforeInsert(Notification notification) {
        if (!checkPaNotificationId( notification.getSender().getPaId() )) {
            throw new IllegalArgumentException("Invalid paID"); // FIXME gestione messaggistica
        }

        String paNotificationId = notification.getPaNotificationId();
        if (!checkPaNotificationId(paNotificationId)) {
            throw new IllegalArgumentException("Invalid paNotificationId"); // FIXME gestione messaggistica
        }

        List<NotificationRecipient> recipients = notification.getRecipients();
        if (!checkRecipients(recipients)) {
            throw new IllegalArgumentException("Invalid recipients"); // FIXME gestione messaggistica
        }

        return notification;
    }
    private boolean checkPaNotificationId(String paNotificationId) {
        return StringUtils.isNotBlank(paNotificationId);
    }

    private boolean checkRecipients(List<NotificationRecipient> recipients) {
        return (recipients != null && !recipients.isEmpty()
                && checkRecipientsItems(recipients));
    }

    private boolean checkRecipientsItems(List<NotificationRecipient> recipients) {
        for (NotificationRecipient recipient : recipients) {
            if (recipient == null || StringUtils.isBlank(recipient.getFc())
                    || (recipient.getPhysicalAddress() == null )) {
                return false;
            }
        }

        return true;
    }

}
