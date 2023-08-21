package it.pagopa.pn.delivery.svc.authorization;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV2;
import lombok.Value;

@Value
public class AuthorizationOutcome {
    boolean authorized;
    NotificationRecipientV2 effectiveRecipient;
    Integer effectiveRecipientIdx;

    public static AuthorizationOutcome fail() {
        return new AuthorizationOutcome( false, null, null );
    }

    public static AuthorizationOutcome ok( NotificationRecipientV2 effectiveRecipient, Integer effectiveRecipientIdx ) {
        return new AuthorizationOutcome( true, effectiveRecipient, effectiveRecipientIdx );
    }
}
