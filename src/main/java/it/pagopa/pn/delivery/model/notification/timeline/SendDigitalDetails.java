package it.pagopa.pn.delivery.model.notification.timeline;

import it.pagopa.pn.delivery.model.notification.address.DigitalAddress;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Setter
@Getter
@Builder
@UserDefinedType
public class SendDigitalDetails extends TimelineElementDetails{

    public SendDigitalDetails(String fc, DigitalAddress address, Integer n, DownstreamId downstreamId, TimelineElement.EventCategory eventCategory){
        super(eventCategory);
        this.fc = fc;
        this.address = address;
        this.n = n;
        this.downstreamId = downstreamId;
    }
    private String fc;
    private DigitalAddress address;
    private Integer n;
    private DownstreamId downstreamId;

}
