package it.pagopa.pn.delivery.model.notification;

import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.delivery.rest.Views;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@Builder
@UserDefinedType
public class NotificationAttachment {

    private Digests digests;
    private String contentType;

    @JsonView(value = { Views.NotificationsView.Send.class })
    private String body;

    @Data
    @UserDefinedType
    public class Digests {
        private String sha256;
    }
}
