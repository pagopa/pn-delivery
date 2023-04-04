package it.pagopa.pn.delivery.svc.authorization;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientPrivate;
import lombok.Value;

@Value
public class AuthorizationOutcome {
    boolean authorized;
    NotificationRecipientPrivate effectiveRecipient;
    Integer effectiveRecipientIdx;

    public static AuthorizationOutcome fail() {
        return new AuthorizationOutcome( false, null, null );
    }

    public static AuthorizationOutcome ok(NotificationRecipientPrivate effectiveRecipient, Integer effectiveRecipientIdx ) {
        return new AuthorizationOutcome( true, effectiveRecipient, effectiveRecipientIdx );
    }
}
