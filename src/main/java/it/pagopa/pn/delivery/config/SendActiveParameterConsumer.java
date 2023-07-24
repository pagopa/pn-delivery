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
public class SendActiveParameterConsumer {

    @Value("${pn.delivery.features.is-send-active-default-value}")
    private Boolean isSendActiveDefaultValue;

    private final ParameterConsumer parameterConsumer;

    private static final String PARAMETER_STORE_MAP_PA_SEND_ACTIVE = "MapPaSendActive";

    public SendActiveParameterConsumer(ParameterConsumer parameterConsumer) {
        this.parameterConsumer = parameterConsumer;
    }

    public Boolean isSendActive( String paTaxId ) {
        log.debug( "Start isSendActive for paTaxId={}", paTaxId );

        Optional<PaTaxIdIsSendActive[]> optionalPaTaxIdIsSendActives = parameterConsumer.getParameterValue(
                PARAMETER_STORE_MAP_PA_SEND_ACTIVE, PaTaxIdIsSendActive[].class);
        if( optionalPaTaxIdIsSendActives.isPresent() ) {
            PaTaxIdIsSendActive[] paTaxIdIsSendActives = optionalPaTaxIdIsSendActives.get();
            for (PaTaxIdIsSendActive paTaxIdIsSendActive : paTaxIdIsSendActives ) {
                if ( paTaxIdIsSendActive.paTaxId.equals(paTaxId) ) {
                    Boolean isSendActive = paTaxIdIsSendActive.isActive;
                    log.debug("paTaxId={} isSendActive={}", paTaxId, isSendActive);
                    return isSendActive;
                }
            }
        }

        log.debug("paTaxId={} configuration not found, isSendActiveDefaultValue={}", paTaxId, isSendActiveDefaultValue);
        return isSendActiveDefaultValue;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class PaTaxIdIsSendActive {
        String paTaxId;
        Boolean isActive;
    }
}
