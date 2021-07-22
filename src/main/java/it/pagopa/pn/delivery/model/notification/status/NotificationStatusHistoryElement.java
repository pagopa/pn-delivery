package it.pagopa.pn.delivery.model.notification.status;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.time.Instant;

@UserDefinedType
public class NotificationStatusHistoryElement {

    private NotificationStatus status;
    private Instant activeFrom;

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public Instant getActiveFrom() {
        return activeFrom;
    }

    public void setActiveFrom(Instant activeFrom) {
        this.activeFrom = activeFrom;
    }
}
