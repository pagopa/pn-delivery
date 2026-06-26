package it.pagopa.pn.delivery.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Function;

/**
 * Internal request object used to consolidate PreLoadRequest and InformalPreLoadRequest
 * into a common format for unified processing within the service layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalPreLoadRequest {
    private String contentType;
    private String sha256;
    private String preloadIdx;
    private Function<String, String> documentTypeResolver;
}
