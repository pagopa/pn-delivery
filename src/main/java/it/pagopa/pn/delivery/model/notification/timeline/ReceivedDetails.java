package it.pagopa.pn.delivery.model.notification.timeline;

import it.pagopa.pn.delivery.model.notification.NotificationAttachment;
import it.pagopa.pn.delivery.model.notification.NotificationRecipient;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.util.List;

@Data
@Builder
@UserDefinedType
public class ReceivedDetails implements TimelineElementDetails {

    private NotificationAttachment.Digests digests;
    private List<NotificationRecipient> recipients;

}
