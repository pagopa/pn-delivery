package it.pagopa.pn.delivery.pnclient.datavault;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.NotificationsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@Slf4j
public class PnDataVaultClientImpl {

    private final RecipientsApi recipientsApi;
    private final NotificationsApi notificationsApi;

    public PnDataVaultClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
        ApiClient newApiClient = new ApiClient( restTemplate );
        newApiClient.setBasePath( cfg.getDataVaultBaseUrl() );

        this.recipientsApi = new RecipientsApi( newApiClient );
        this.notificationsApi = new NotificationsApi( newApiClient );
    }

    public String ensureRecipientByExternalId(RecipientType recipientType, String taxId ){
        return recipientsApi.ensureRecipientByExternalId( recipientType, taxId );
    }

    public void updateNotificationAddressesByIun(String iun, List<NotificationRecipientAddressesDto> notificationRecipientAddressesDto) {
        notificationsApi.updateNotificationAddressesByIun( iun, null, notificationRecipientAddressesDto );
    }

    public List<BaseRecipientDto> getRecipientDenominationByInternalId(List<String> internalId) {
        return recipientsApi.getRecipientDenominationByInternalId( internalId );
    }

    public List<NotificationRecipientAddressesDto> getNotificationAddressesByIun(String iun) {
        return notificationsApi.getNotificationAddressesByIun( iun, null );
    }

}
