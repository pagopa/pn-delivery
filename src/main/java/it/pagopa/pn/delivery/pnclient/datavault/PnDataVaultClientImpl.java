package it.pagopa.pn.delivery.pnclient.datavault;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.NotificationsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@CustomLog
@RequiredArgsConstructor
public class PnDataVaultClientImpl {

    private final RecipientsApi recipientsApi;
    private final NotificationsApi notificationsApi;


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

}
