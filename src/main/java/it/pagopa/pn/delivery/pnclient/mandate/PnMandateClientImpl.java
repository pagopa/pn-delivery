package it.pagopa.pn.delivery.pnclient.mandate;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.api.MandatePrivateServiceApi;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class PnMandateClientImpl {

    private final MandatePrivateServiceApi mandatesApi;

    public PnMandateClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getMandateBaseUrl());
        this.mandatesApi = new MandatePrivateServiceApi(newApiClient);
    }

    public List<InternalMandateDto> listMandatesByDelegate(String delegated, String mandateId, CxTypeAuthFleet cxType, List<String> cxGroups) {
        log.debug("Start get mandates for delegated={} and mandateId={}", delegated, mandateId);
        return mandatesApi.listMandatesByDelegate(delegated, cxType, mandateId, cxGroups);
    }

    public List<InternalMandateDto> listMandatesByDelegator(String delegator, String mandateId,
                                                            CxTypeAuthFleet cxType, List<String> cxGroups, String cxRole) {
        log.debug("Start get mandates for delegator={} and mandateId={}", delegator, mandateId);
        return mandatesApi.listMandatesByDelegator(delegator, cxType, mandateId, cxGroups, cxRole);
    }

}
