package it.pagopa.pn.delivery.model.notification;

import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.delivery.rest.Views;

public class NotificationAttachment {

    private Digests digests = new Digests();
    private String contentType;

    @JsonView(value = { Views.NotificationsView.Send.class })
    private String body;

    public Digests getDigests() {
        return digests;
    }

    public void setDigests(Digests digests) {
        this.digests = digests;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }


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
