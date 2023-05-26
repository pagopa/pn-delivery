package it.pagopa.pn.delivery.pnclient.externalregistries;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.PaymentInfoApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaymentInfo;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@CustomLog
@Component
@RequiredArgsConstructor
public class PnExternalRegistriesClientImpl {

    private final PaymentInfoApi paymentInfoApi;
    private final InternalOnlyApi internalOnlyApi;


    public PaymentInfo getPaymentInfo(String paTaxId, String noticeNumber ) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "getPaymentInfo");
        return paymentInfoApi.getPaymentInfo( paTaxId, noticeNumber );
    }

    public List<PaGroup> getGroups(String senderId) {
        try {
            log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "getGroups");
            return internalOnlyApi.getAllGroupsPrivate(senderId, null);
        } catch (Exception exc) {
            log.error("Error during retrieve of the groups", exc);
            return Collections.emptyList();
        }
    }
}
