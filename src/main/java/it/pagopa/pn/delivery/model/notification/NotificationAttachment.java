package it.pagopa.pn.delivery.model.notification;

import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.delivery.rest.JsonViews;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@Builder
@UserDefinedType
public class NotificationAttachment {

    private Digests digests;
    private String contentType;

    @JsonView(value = { JsonViews.NotificationsView.ReceivedNotification.class })
    private String body;

    @Data
    @UserDefinedType
    public class Digests {
        private String sha256;
    }
}
