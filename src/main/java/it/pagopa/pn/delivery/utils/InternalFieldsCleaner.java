package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV2;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;

public class InternalFieldsCleaner {

    private InternalFieldsCleaner() {}


    public static void cleanInternalId(List<NotificationRecipientV2> recipients) {
        if(! CollectionUtils.isEmpty(recipients)) {
            recipients.forEach(InternalFieldsCleaner::cleanInternalId);
        }
    }

    public static void cleanInternalId(NotificationRecipientV2 recipient) {
        if(recipient != null) {
            recipient.setInternalId(null);
        }
    }

    public static void cleanInternalFields(InternalNotification internalNotification) {
        if( !ObjectUtils.isEmpty(internalNotification) ) {
            internalNotification.setRecipientIds( null );
            internalNotification.setSourceChannel( null );
            cleanInternalId( internalNotification.getRecipients() );
        }
    }
}
