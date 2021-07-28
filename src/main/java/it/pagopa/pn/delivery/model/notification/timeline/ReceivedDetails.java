package it.pagopa.pn.delivery.model.notification.timeline;

import it.pagopa.pn.delivery.model.notification.NotificationAttachment;
import it.pagopa.pn.delivery.model.notification.NotificationRecipient;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.util.List;

@Getter
@Setter
@UserDefinedType
public class ReceivedDetails extends TimelineElementDetails {

    public ReceivedDetails(TimelineElement.EventCategory eventCategory){
        super(eventCategory);
    }
    private NotificationAttachment.Digests digests;
    private List<NotificationRecipient> recipients;

}
