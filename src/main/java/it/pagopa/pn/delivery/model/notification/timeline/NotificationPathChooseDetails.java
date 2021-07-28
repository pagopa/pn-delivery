package it.pagopa.pn.delivery.model.notification.timeline;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Setter
@Getter
@Builder
@UserDefinedType
public class NotificationPathChooseDetails extends TimelineElementDetails{

    public NotificationPathChooseDetails(TimelineElement.EventCategory eventCategory){
        super(eventCategory);
    }
    public enum DeliveryMode {
        DIGITAL,
        ANALOG
    }

    private String fc;
    private DeliveryMode deliveryMode;

}
