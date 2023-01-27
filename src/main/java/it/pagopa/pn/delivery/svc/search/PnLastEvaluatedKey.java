package it.pagopa.pn.delivery.svc.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.Base64Utils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_LAST_EVALUATED_KEY;


public class PnLastEvaluatedKey {

    private static final ObjectWriter objectWriter = new ObjectMapper().writerFor( KeyPair.class );
    private static final ObjectReader objectReader = new ObjectMapper().readerFor( KeyPair.class );

    @ToString.Include
    private String externalLastEvaluatedKey;
    @ToString.Include
    private Map<String, AttributeValue> internalLastEvaluatedKey;


    public String getExternalLastEvaluatedKey() {
        return externalLastEvaluatedKey;
    }

    public void setExternalLastEvaluatedKey(String externalLastEvaluatedKey) {
        this.externalLastEvaluatedKey = externalLastEvaluatedKey;
    }

    public Map<String, AttributeValue> getInternalLastEvaluatedKey() {
        return internalLastEvaluatedKey;
    }

    public void setInternalLastEvaluatedKey(Map<String, AttributeValue> internalLastEvaluatedKey) {
        this.internalLastEvaluatedKey = internalLastEvaluatedKey;
    }

    public static PnLastEvaluatedKey deserializeInternalLastEvaluatedKey( String encodedString ) throws JsonProcessingException {
        String jsonString = new String( Base64Utils.decodeFromUrlSafeString( encodedString ), StandardCharsets.UTF_8 );
        KeyPair keyPair = objectReader.readValue( jsonString );
        PnLastEvaluatedKey pnLastEvaluatedKey = new PnLastEvaluatedKey();
        pnLastEvaluatedKey.setExternalLastEvaluatedKey( keyPair.getEk() );
        pnLastEvaluatedKey.setInternalLastEvaluatedKey( keyPair.ik2dynamo() );
        return pnLastEvaluatedKey;
    }

    public String serializeInternalLastEvaluatedKey( ) {
        Map<String,String> internalAttributesValues = new HashMap<>();
        for (Map.Entry<String,AttributeValue> entry : this.internalLastEvaluatedKey.entrySet()) {
            internalAttributesValues.put(entry.getKey(), entry.getValue().s());
        }
        KeyPair toSerialize = new KeyPair( this.externalLastEvaluatedKey, internalAttributesValues );
        String result;
        try {
            result = objectWriter.writeValueAsString( toSerialize );
            result = Base64Utils.encodeToUrlSafeString( result.getBytes(StandardCharsets.UTF_8) );
        } catch ( JsonProcessingException e ) {
            throw new PnInternalException( "Unable to serialize internal LastEvaluatedKey",
                    ERROR_CODE_DELIVERY_UNSUPPORTED_LAST_EVALUATED_KEY,
                    e );
        }
        return result;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KeyPair {
        private String ek;
        private Map<String,String> ik;

        private Map<String,AttributeValue> ik2dynamo() {
            Map<String,AttributeValue> result = new HashMap<>();
            for (Map.Entry<String,String> entry : this.ik.entrySet()) {
                result.put(entry.getKey(),
                        AttributeValue.builder()
                                .s( entry.getValue() )
                                .build());
            }
            return result;
        }
    }
}
