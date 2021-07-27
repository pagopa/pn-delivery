package it.pagopa.pn.delivery.model.notification.timeline;

import it.pagopa.pn.delivery.model.notification.address.DigitalAddress;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@Builder
@UserDefinedType
public class SendDigitalDetails {

    private String fc;
    private DigitalAddress address;
    private Integer n;
    private DownstreamId downstreamId;

}
