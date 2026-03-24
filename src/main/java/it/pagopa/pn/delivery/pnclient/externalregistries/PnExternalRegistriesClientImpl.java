package it.pagopa.pn.delivery.pnclient.externalregistries;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.exception.PnRootIdNonFountException;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.InfoPaApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.PaymentInfoApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.RootSenderIdApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.*;
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
    private final InfoPaApi infoPaApi;

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

    @Cacheable(value = "aooSenderIdCache", cacheManager = "pnCacheManager")
    public String getRootSenderId(String senderId){
        try{
            RootSenderIdResponse rootSenderIdPrivate = rootSenderIdApi.getRootSenderIdPrivate(senderId);
            return rootSenderIdPrivate.getRootId();
        }catch (Exception exc) {
            String message = String.format("Error during map rootSenderID = %s [exception received = %s]", senderId, exc);
            log.error(message);
            throw new PnRootIdNonFountException(message);
        }
    }

    public PaInfo getOnePa(String paId){
            log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "getOnePa");
            return infoPaApi.getOnePa(paId);
    }
}
