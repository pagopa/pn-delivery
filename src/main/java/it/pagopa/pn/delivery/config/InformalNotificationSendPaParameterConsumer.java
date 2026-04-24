package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Slf4j
@Configuration
public class InformalNotificationSendPaParameterConsumer {

    @Value("${pn.delivery.features.is-send-informal-active-default-value}")
    private Boolean isSendInformalActiveDefaultValue;

    private final ParameterConsumer parameterConsumer;

    private static final String PARAMETER_STORE_MAP_CXID_IS_INFORMAL = "InformalNotificationSendPaWhiteList";

    public InformalNotificationSendPaParameterConsumer(ParameterConsumer parameterConsumer) {
        this.parameterConsumer = parameterConsumer;
    }

    public Boolean isSenderActiveForInformalNotification(String cxId ) {
        log.debug( "Start isSendActive for cxId={}", cxId );

        Optional<CxIdIsInformalActive[]> optionalCxIdIsInformalActives = parameterConsumer.getParameterValue(
                PARAMETER_STORE_MAP_CXID_IS_INFORMAL, CxIdIsInformalActive[].class);
        if( optionalCxIdIsInformalActives.isPresent() ) {
            CxIdIsInformalActive[] cxIdIsInformalActives = optionalCxIdIsInformalActives.get();
            for (CxIdIsInformalActive cxIdIsInformalActive : cxIdIsInformalActives ) {
                if ( cxIdIsInformalActive.cxId.equals(cxId) ) {
                    Boolean isInformalActive = cxIdIsInformalActive.isActive;
                    log.debug("cxId={} isInformalActive={}", cxId, isInformalActive);
                    return isInformalActive;
                }
            }
        }

        log.debug("cxId={} configuration not found, isSendInformalActiveDefaultValue={}", cxId, isSendInformalActiveDefaultValue);
        return isSendInformalActiveDefaultValue;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class CxIdIsInformalActive {
        String cxId;
        Boolean isActive;
    }
}
