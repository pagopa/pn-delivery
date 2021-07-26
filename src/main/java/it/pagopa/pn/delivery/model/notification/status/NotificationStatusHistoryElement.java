package it.pagopa.pn.delivery.model.notification.status;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.time.Instant;

@Data
@Builder
@UserDefinedType
public class NotificationStatusHistoryElement {

    private NotificationStatus status;
    private Instant activeFrom;
}
