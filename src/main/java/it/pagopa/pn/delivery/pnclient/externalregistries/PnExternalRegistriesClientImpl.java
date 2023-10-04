package it.pagopa.pn.delivery.pnclient.externalregistries;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.PaymentInfoApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.RootSenderIdApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroupStatus;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.RootSenderIdResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@CustomLog
@Component
@RequiredArgsConstructor
public class PnExternalRegistriesClientImpl {

    private final PaymentInfoApi paymentInfoApi;
    private final InternalOnlyApi internalOnlyApi;
    private final RootSenderIdApi rootSenderIdApi;

    public PaymentInfo getPaymentInfo(String paTaxId, String noticeNumber ) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "getPaymentInfo");
        return paymentInfoApi.getPaymentInfo( paTaxId, noticeNumber );
    }

    public List<PaGroup> getGroups(String senderId, boolean onlyActive) {
        try {
            log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "getGroups");
            return internalOnlyApi.getAllGroupsPrivate(senderId, onlyActive? PaGroupStatus.ACTIVE:null);
        } catch (Exception exc) {
            log.error("Error during retrieve of the groups", exc);
            return Collections.emptyList();
        }
    }

    @Cacheable("aooSenderIdCache")
    public String getRootSenderId(String senderId){
        try{
            RootSenderIdResponse rootSenderIdPrivate = rootSenderIdApi.getRootSenderIdPrivate(senderId);
            return rootSenderIdPrivate.getRootId();
        }catch (Exception exc) {
            log.error("Error during map rootSenderID", exc);
            return "";
        }
    }
}
