package it.pagopa.pn.delivery.model.notification.timeline;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@Builder
@UserDefinedType
public class NotificationPathChooseDetails {

    public enum DeliveryMode {
        DIGITAL,
        ANALOG
    }

    private String fc;
    private DeliveryMode deliveryMode;

}
