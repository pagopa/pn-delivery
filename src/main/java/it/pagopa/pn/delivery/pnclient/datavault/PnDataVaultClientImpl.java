package it.pagopa.pn.delivery.pnclient.datavault;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes;
import it.pagopa.pn.delivery.exception.PnDeliveryMessageNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.MessagesApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.NotificationsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@CustomLog
@RequiredArgsConstructor
public class PnDataVaultClientImpl {

    private final RecipientsApi recipientsApi;
    private final NotificationsApi notificationsApi;
    private final MessagesApi messagesApi;

    public String ensureRecipientByExternalId(RecipientType recipientType, String taxId ){
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "ensureRecipientByExternalId");
        return recipientsApi.ensureRecipientByExternalId( recipientType, taxId );
    }

    public void updateNotificationAddressesByIun(String iun, List<NotificationRecipientAddressesDto> notificationRecipientAddressesDto) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "updateNotificationAddressesByIun");
        notificationsApi.updateNotificationAddressesByIun( iun, null, notificationRecipientAddressesDto );
    }

    public List<BaseRecipientDto> getRecipientDenominationByInternalId(List<String> internalId) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "getRecipientDenominationByInternalId");
        return recipientsApi.getRecipientDenominationByInternalId( internalId );
    }

    public List<NotificationRecipientAddressesDto> getNotificationAddressesByIun(String iun) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "getNotificationAddressesByIun");
        return notificationsApi.getNotificationAddressesByIun( iun, null );
    }

    public MessageResponseDto createInformalMessage(MessageRequestDto messageRequestDto) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "createInformalMessage");
        return messagesApi.createMessage(messageRequestDto);
    }

    public MessageResponseDto getInformalMessageById(UUID messageId, UUID senderId) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "getInformalMessageById");
        try {
            return messagesApi.getMessageById(messageId, senderId);
        } catch (Exception exception) {
            if (exception instanceof PnHttpResponseException pnHttpResponseException
                    && pnHttpResponseException.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                log.error("Message with messageId {} and senderId {} does not match the senderId or was not found", messageId, senderId);
                throw new PnDeliveryMessageNotFoundException("Message does not match the senderId or was not found"
                        , "MessageId does not belong to the requesting senderId or does not exist",
                        PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_MESSAGE_NOT_FOUND);
            }
            throw exception;
        }
    }
}
