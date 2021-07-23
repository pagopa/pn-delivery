package it.pagopa.pn.delivery.model.notification;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@UserDefinedType
public class NotificationSender {

    private String paId;
    private String paName;

}
