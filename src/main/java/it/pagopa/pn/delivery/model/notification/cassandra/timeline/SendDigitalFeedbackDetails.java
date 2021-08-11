package it.pagopa.pn.delivery.model.notification.cassandra.timeline;

import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import lombok.*;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.util.List;

@Getter
@Setter
@UserDefinedType
public class SendDigitalFeedbackDetails extends SendDigitalDetails implements TimelineElementDetails{

    private List<String> errors;

    public SendDigitalFeedbackDetails(String fc, DigitalAddress address, Integer n, DownstreamId downstreamId) {
        super(fc, address, n, downstreamId);
    }

}
