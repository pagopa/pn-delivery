package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.search.AllowedAdditionalLanguages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class InformalMessageService {
    private final PnDataVaultClientImpl pnDataVaultClient;
    private final PnDeliveryConfigs pnDeliveryConfigs;

    public MessageResponseDto createInformalMessage(MessageRequestDto requestDto) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_COM_MSG_INSERT, "createInformalMessage senderId={}", requestDto.getSenderId())
                .build();
        logEvent.log();
        try {
            if (requestDto.getPrimaryContent().getLanguage() == null || !"IT".equalsIgnoreCase(requestDto.getPrimaryContent().getLanguage().getValue())) {
                logEvent.generateFailure("Primary message language must be IT", PnAuditLogEventType.AUD_COM_MSG_INSERT);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primary message language must be IT");
            }
            if (requestDto.getSecondaryContent() != null) {
                String addLang = requestDto.getSecondaryContent().getLanguage().getValue();
                if (!isValidAdditionalLanguage(addLang)) {
                    logEvent.generateFailure("Additional message language must be FR, SL o DE", PnAuditLogEventType.AUD_COM_MSG_INSERT);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Additional message language must be FR, SL o DE");
                }
            }
            // Validazione: somma lunghezze body
            int longBodyLen = 0;
            int shortBodyLen = 0;
            longBodyLen += requestDto.getPrimaryContent().getLongBody().length();
            if (requestDto.getPrimaryContent().getShortBody() != null)
                shortBodyLen += requestDto.getPrimaryContent().getShortBody().length();
            if (requestDto.getSecondaryContent() != null) {
                longBodyLen += requestDto.getSecondaryContent().getLongBody().length();
                if (requestDto.getSecondaryContent().getShortBody() != null)
                    shortBodyLen += requestDto.getSecondaryContent().getShortBody().length();
            }
            if ((pnDeliveryConfigs.getMaxMessageLongBodyLength() != null && longBodyLen > pnDeliveryConfigs.getMaxMessageLongBodyLength()) ||
                (pnDeliveryConfigs.getMaxMessageShortBodyLength() != null && shortBodyLen > pnDeliveryConfigs.getMaxMessageShortBodyLength())) {
                logEvent.generateFailure("Body length exceeds max allowed", PnAuditLogEventType.AUD_COM_MSG_INSERT);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body length exceeds max allowed");
            }
            logEvent.generateSuccess("createInformalMessage",PnAuditLogEventType.AUD_COM_MSG_INSERT);
            return pnDataVaultClient.createInformalMessage(requestDto);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            logEvent.generateFailure(ex.getMessage(), PnAuditLogEventType.AUD_COM_MSG_INSERT);
            throw ex;
        }
    }

    private boolean isValidAdditionalLanguage(String lang) {
        return Arrays.stream(AllowedAdditionalLanguages.values())
                .map(AllowedAdditionalLanguages::name)
                .anyMatch(lang::equals);
    }

    public MessageResponseDto getInformalMessageById(UUID messageId, UUID senderId) {
        return pnDataVaultClient.getInformalMessageById(messageId, senderId);
    }
}
