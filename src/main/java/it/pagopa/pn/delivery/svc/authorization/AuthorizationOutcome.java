package it.pagopa.pn.delivery.svc.authorization;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import lombok.Value;

@Value
public class AuthorizationOutcome {
    boolean authorized;
    NotificationRecipient effectiveRecipient;
    Integer effectiveRecipientIdx;

    public static AuthorizationOutcome fail() {
        return new AuthorizationOutcome( false, null, null );
    }

    public static AuthorizationOutcome ok( NotificationRecipient effectiveRecipient, Integer effectiveRecipientIdx ) {
        return new AuthorizationOutcome( true, effectiveRecipient, effectiveRecipientIdx );
    }
}
