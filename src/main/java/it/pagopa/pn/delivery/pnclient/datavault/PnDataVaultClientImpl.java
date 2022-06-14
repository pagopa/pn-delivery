package it.pagopa.pn.delivery.pnclient.datavault;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.api.RecipientsApi;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class PnDataVaultClientImpl {

    private final RecipientsApi recipientsApi;

    public PnDataVaultClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
        ApiClient newApiClient = new ApiClient( restTemplate );
        newApiClient.setBasePath( cfg.getDataVaultBaseUrl() );

        this.recipientsApi = new RecipientsApi( newApiClient );
    }

public String ensureRecipientByExternalId(RecipientType recipientType, String taxId ){
        return recipientsApi.ensureRecipientByExternalId( recipientType, taxId );
}

}
