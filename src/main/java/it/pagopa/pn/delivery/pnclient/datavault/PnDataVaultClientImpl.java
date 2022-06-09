package it.pagopa.pn.delivery.pnclient.datavault;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.api.NotificationsApi;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.api.RecipientsApi;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
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
    	log.debug("Starting ensureRecipientByExternalId with recipientType={} and taxId={}", recipientType, taxId);
        return recipientsApi.ensureRecipientByExternalId( recipientType, taxId );
    }

    public void updateNotificationAddressesByIun(String iun, List<NotificationRecipientAddressesDto> notificationRecipientAddressesDto) {
    	log.debug("Starting updateNotificationAddressesByIun with iun={} and notificationRecipientAddressesDto={}", iun, notificationRecipientAddressesDto);
    	notificationsApi.updateNotificationAddressesByIun( iun, notificationRecipientAddressesDto );
    }

    public List<BaseRecipientDto> getRecipientDenominationByInternalId(List<String> internalId) {
    	log.debug("Starting getRecipientDenominationByInternalId with internalIds={}", internalId);
        return recipientsApi.getRecipientDenominationByInternalId( internalId );
    }

    public List<NotificationRecipientAddressesDto> getNotificationAddressesByIun(String iun) {
    	log.debug("Starting getNotificationAddressesByIun with iun={}", iun);
        return notificationsApi.getNotificationAddressesByIun( iun );
    }

}
