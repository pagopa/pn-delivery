package it.pagopa.pn.delivery.model.notification.cassandra.address;

import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@Builder
@UserDefinedType
public class DigitalAddress {

    private DigitalAddressType type;
    private String address;

}
