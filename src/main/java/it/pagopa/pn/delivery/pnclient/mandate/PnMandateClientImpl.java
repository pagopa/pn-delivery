package it.pagopa.pn.delivery.pnclient.mandate;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.api.MandatePrivateServiceApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.DelegateType;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.MandateByDelegatorRequestDto;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@CustomLog
@Component
@RequiredArgsConstructor
public class PnMandateClientImpl {

    private final MandatePrivateServiceApi mandatesApi;


    public List<InternalMandateDto> listMandatesByDelegate(String delegated, String mandateId, CxTypeAuthFleet cxType, List<String> cxGroups) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_MANDATE, "listMandatesByDelegate");
        return mandatesApi.listMandatesByDelegate(delegated, cxType, mandateId, cxGroups);
    }

    public List<InternalMandateDto> listMandatesByDelegator(String delegator, String mandateId,
                                                            CxTypeAuthFleet cxType, List<String> cxGroups, String cxRole,
                                                            DelegateType delegateType) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_MANDATE, "listMandatesByDelegator");
        return mandatesApi.listMandatesByDelegator(delegator, cxType, mandateId, cxGroups, cxRole, delegateType);
    }

    public List<InternalMandateDto> listMandatesByDelegators(DelegateType delegateType, List<String> cxGroups, List<MandateByDelegatorRequestDto> requestBody) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_MANDATE, "listMandatesByDelegators");
        return mandatesApi.listMandatesByDelegators(delegateType, cxGroups, requestBody);
    }

}
