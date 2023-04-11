package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class InternalIdCleaner {

    private InternalIdCleaner() {}


    public static void cleanInternalId(List<NotificationRecipient> recipients) {
        if(! CollectionUtils.isEmpty(recipients)) {
            recipients.forEach(InternalIdCleaner::cleanInternalId);
        }
    }

    public static void cleanInternalId(NotificationRecipient recipient) {
        if(recipient != null) {
            recipient.setInternalId(null);
        }
    }
}
