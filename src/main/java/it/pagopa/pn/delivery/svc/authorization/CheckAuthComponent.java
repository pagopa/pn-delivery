package it.pagopa.pn.delivery.svc.authorization;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class CheckAuthComponent {
    private final PnMandateClientImpl pnMandateClient;

    public CheckAuthComponent(PnMandateClientImpl pnMandateClient) {
        this.pnMandateClient = pnMandateClient;
    }

    public AuthorizationOutcome canAccess(ReadAccessAuth action, InternalNotification notification) {
        if ( !Objects.equals( action.getIun() , notification.getIun()) ) {
            throw new IllegalArgumentException( "" );
        }
        CxType cxType = action.getCxType();

        AuthorizationOutcome authorized;
        switch ( cxType ) {
            case PA: authorized = paCanAccess( action, notification ); break;
            case PF: authorized = pfCanAccess( action, notification ); break;
            default: throw new PnInternalException( "Unsupported cxType="+ cxType );
        }
        return authorized;
    }

    private AuthorizationOutcome pfCanAccess(ReadAccessAuth action, InternalNotification notification) {
        String cxId = action.getCxId();
        log.debug( "Check if cxId={} can access documents iun={}", cxId, notification.getIun() );
        int rIdx = notification.getRecipientIds().indexOf(cxId);
        Integer recipientIdx = (rIdx >= 0 ? rIdx : null);

        // gestione deleghe
        String mandateId = action.getMandateId();
        if (recipientIdx == null && mandateId != null) {
            log.debug( "Check validity mandateId={} cxId={} iun={}", mandateId, cxId, notification.getIun() );
            List<InternalMandateDto> mandates = this.pnMandateClient.listMandatesByDelegate(cxId, mandateId);
            if(!mandates.isEmpty()
                    && notification.getSentAt().isAfter( OffsetDateTime.parse( Objects.requireNonNull(mandates.get(0).getDatefrom()) ))
            ) {
                String delegatedCxId = mandates.get(0).getDelegator();
                rIdx = notification.getRecipientIds().indexOf( delegatedCxId );
                recipientIdx = (rIdx >= 0 ? rIdx : null);
            }
            else
            {
                String message = String.format("Unable to find any mandate for delegate=%s with mandateId=%s", cxId, mandateId);
                log.error( message );
                throw new PnMandateNotFoundException( message );
            }
        }

        NotificationRecipient effectiveRecipient = null;
        if (recipientIdx != null ) {
            effectiveRecipient = notification.getRecipients().get( recipientIdx );
        }

        AuthorizationOutcome authorized;
        if ( effectiveRecipient != null ) {
            authorized = AuthorizationOutcome.ok( effectiveRecipient, recipientIdx );
        } else
        {
            authorized = AuthorizationOutcome.fail();
        }
        return authorized;
    }

    private AuthorizationOutcome paCanAccess(ReadAccessAuth action, InternalNotification notification) {
        String senderId = action.getCxId();
        log.debug( "Check if senderId={} can access iun={}", senderId, notification.getIun() );
        boolean authorized = senderId.equals( notification.getSenderPaId() );
        AuthorizationOutcome result;
        if ( authorized ) {
            Integer recipientIdx = action.getRecipientIdx();
            NotificationRecipient effectiveRecipient = null;
            if ( recipientIdx != null && recipientIdx >= 0 ) {
                effectiveRecipient = notification.getRecipients().get(recipientIdx);
            }
            result = AuthorizationOutcome.ok(effectiveRecipient, recipientIdx);
        } else {
            result = AuthorizationOutcome.fail();
        }
        return result;
    }
}
