package it.pagopa.pn.delivery.model.notification.status;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@UserDefinedType
public enum NotificationStatus {
    RECEIVED,
    DELIVERING,
    DELIVERED,
    VIEWED,
    EFFECTIVE_DATE,
    PAID
}
