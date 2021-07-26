package it.pagopa.pn.delivery.model.notification.address;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@Builder
@UserDefinedType
public class DigitalAddress {

    public enum Type {
        PEC
    }

    private Type type;
    private String address;

}
