package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileCreationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal response object that wraps the SafeStorage response data.
 * Used to provide a unified response format from the preloadDocumentsInternal method.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalPreLoadResponse {
    private String uploadUrl;
    private String key;
    private FileCreationResponse.UploadMethodEnum uploadMethod;
    private String secret;
    private String preloadIdx;
}
