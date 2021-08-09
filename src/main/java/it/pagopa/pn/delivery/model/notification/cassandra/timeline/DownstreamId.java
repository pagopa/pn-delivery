package it.pagopa.pn.delivery.model.notification.cassandra.timeline;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@UserDefinedType
public class DownstreamId {

    private String systemId;
    private String messageId;

}
