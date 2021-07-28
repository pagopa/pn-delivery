package it.pagopa.pn.delivery.model.notification.timeline;

import it.pagopa.pn.delivery.model.notification.address.DigitalAddress;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.util.List;

@Builder
@UserDefinedType
public class SendDigitalFeedbackDetails extends SendDigitalDetails {

    private List<String> errors;

    SendDigitalFeedbackDetails(String fc, DigitalAddress address, Integer n, DownstreamId downstreamId, TimelineElement.EventCategory eventCategory) {
        super(fc, address, n, downstreamId, eventCategory);
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
