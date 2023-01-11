package it.pagopa.pn.delivery.svc.authorization;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_CX_TYPE;

@Component
@Slf4j
public class CheckAuthComponent {
    private final PnMandateClientImpl pnMandateClient;

    public CheckAuthComponent(PnMandateClientImpl pnMandateClient) {
        this.pnMandateClient = pnMandateClient;
    }

    public AuthorizationOutcome canAccess(ReadAccessAuth action, InternalNotification notification) {
        if (!Objects.equals(action.getIun(), notification.getIun())) {
            throw new IllegalArgumentException("");
        }
        CxType cxType = action.getCxType();

        return switch (cxType) {
            case PA -> paCanAccess(action, notification);
            case PF -> pfCanAccess(action, notification);
            default -> throw new PnInternalException("Unsupported cxType=" + cxType, ERROR_CODE_DELIVERY_UNSUPPORTED_CX_TYPE);
        };
    }

    private AuthorizationOutcome pfCanAccess(ReadAccessAuth action, InternalNotification notification) {
        String cxId = action.getCxId();
        log.debug( "Check if cxId={} can access documents iun={}", cxId, notification.getIun() );
        int rIdx = notification.getRecipientIds().indexOf(cxId);
        Integer recipientIdx = getRecipientIdx(rIdx);

        // gestione deleghe
        String mandateId = action.getMandateId();
        if (recipientIdx == null && mandateId != null) {
            log.debug( "Check validity mandateId={} cxId={} iun={}", mandateId, cxId, notification.getIun() );
            List<InternalMandateDto> mandates = this.pnMandateClient.listMandatesByDelegate(cxId, mandateId, null, null); // FIXME cxType cxGroups
            if(mandates.isEmpty() ||
                    OffsetDateTime.parse( Objects.requireNonNull(mandates.get(0).getDatefrom()) ).isAfter( notification.getSentAt() )
            ) {
                String message = String.format("Unable to find any mandate for delegate=%s with mandateId=%s", cxId, mandateId);
                log.error( message );
                throw new PnMandateNotFoundException( message );
            }
            String delegatedCxId = mandates.get(0).getDelegator();
            rIdx = notification.getRecipientIds().indexOf( delegatedCxId );
            recipientIdx = getRecipientIdx(rIdx);
            log.info("pfCanAccess iun={} delegatorId={} recipiendIdx={}", notification.getIun(), delegatedCxId, recipientIdx);
        }

        NotificationRecipient effectiveRecipient = null;
        if (recipientIdx != null ) {
            effectiveRecipient = notification.getRecipients().get( recipientIdx );
            log.info("pfCanAccess iun={} effectiveRecipient={} recipient_size={}", notification.getIun(), effectiveRecipient==null?"NULL effective recipient":effectiveRecipient.getInternalId(), notification.getRecipients().size());
            if (effectiveRecipient == null)
            {
                notification.getRecipients().forEach(x -> log.info("pfCanAccess list of recipient iun={} recipient={}", notification.getIun(), x==null?"NULL!":x.getInternalId()));
            }
        }

        return Objects.nonNull( effectiveRecipient ) ?
                AuthorizationOutcome.ok( effectiveRecipient, recipientIdx ) : AuthorizationOutcome.fail();
    }

    @Nullable
    private Integer getRecipientIdx(int rIdx) {
        return rIdx >= 0 ? rIdx : null;
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
