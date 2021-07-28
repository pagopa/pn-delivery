package it.pagopa.pn.delivery.model.notification.timeline;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Builder
@Data
@UserDefinedType
public class TimelineElementDetails {

    protected TimelineElement.EventCategory eventCategory ;

    public TimelineElementDetails(TimelineElement.EventCategory eventCategory) {
        this.eventCategory = eventCategory;
    }
}
