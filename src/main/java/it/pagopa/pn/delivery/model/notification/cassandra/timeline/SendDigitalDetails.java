package it.pagopa.pn.delivery.model.notification.cassandra.timeline;

import it.pagopa.pn.api.dto.notification.address.DigitalAddress;

import lombok.Builder;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Getter
@Setter
@Builder
@UserDefinedType
public class SendDigitalDetails implements TimelineElementDetails{

    public SendDigitalDetails(String fc, DigitalAddress address, Integer n, DownstreamId downstreamId){
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
