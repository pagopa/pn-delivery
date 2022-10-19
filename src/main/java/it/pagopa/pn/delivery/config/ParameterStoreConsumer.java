package it.pagopa.pn.delivery.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public interface ParameterStoreConsumer extends ParameterConsumer {

    String PARAMETER_STORE_MAP_PA_NAME = "testMapPAMVP";

    default Boolean isMVPForPA( String paTaxId ) {
        String parameterValue = this.getParameter( PARAMETER_STORE_MAP_PA_NAME );
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<PaTaxIdIsMVP> paTaxIdIsMVPList = objectMapper.readValue( parameterValue, new TypeReference<List<PaTaxIdIsMVP>>() {} );
            for ( PaTaxIdIsMVP value : paTaxIdIsMVPList ) {
                if ( paTaxId.equals( value.getPaTaxId() )) {
                    return value.getIsMVP();
                }
            }
        } catch ( JsonProcessingException ex ) {
            return false;
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class PaTaxIdIsMVP {
        String paTaxId;
        Boolean isMVP;
    }

}
