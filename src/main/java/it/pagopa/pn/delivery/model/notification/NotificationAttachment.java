package it.pagopa.pn.delivery.model.notification;

import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.delivery.rest.Views;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@UserDefinedType
public class NotificationAttachment {

    private Digests digests = new Digests();
    private String contentType;

    @JsonView(value = { Views.NotificationsView.Send.class })
    private String body;

    @UserDefinedType
    public class Digests {
        private String sha256;

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }
    }
}
