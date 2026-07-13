package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequest;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.UUID;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_GENERIC_ERROR;
import static it.pagopa.pn.delivery.models.internal.notification.mapper.LocalizedContentMapper.mapToServerLocalizedContent;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageEnricher {
    private final PnDataVaultClientImpl pnDataVaultClient;

    public void enrichInternalNotification(InternalNotification notification) {
        if (notification.getRecipients() == null || notification.getRecipients().isEmpty()) {
            return;
        }
        for (NotificationRecipient recipient : notification.getRecipients()) {
            MessageResponseDto messageDto = retrieveInformalMessageById(recipient, notification);

            recipient.setMessage(
                    NewMessageRequest.builder()
                            .primaryMessage(mapToServerLocalizedContent(messageDto.getPrimaryContent()))
                            .additionalMessage(mapToServerLocalizedContent(messageDto.getSecondaryContent()))
                            .build()
            );
        }
    }

    private @Nonnull MessageResponseDto retrieveInformalMessageById(NotificationRecipient recipient, InternalNotification notification) {
        try {
            return pnDataVaultClient.getInformalMessageById(
                    UUID.fromString(recipient.getMessageId()),
                    UUID.fromString(notification.getSenderPaId())
            );
        } catch (Exception ex) {
            String message = String.format(
                    "Couldn't retrieve notification message associated to recipient with id=%s, messageId=%s, senderPaId=%s",
                    recipient.getInternalId(),
                    recipient.getMessageId(),
                    notification.getSenderPaId()
            );
            throw new PnInternalException(message, ERROR_CODE_DELIVERY_GENERIC_ERROR, ex);
        }
    }
}
