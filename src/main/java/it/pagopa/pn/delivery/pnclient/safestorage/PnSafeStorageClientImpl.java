package it.pagopa.pn.delivery.pnclient.safestorage;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class PnSafeStorageClientImpl {

    private final FileDownloadApi fileDownloadApi;
    private final FileUploadApi fileUploadApi;

    public PnSafeStorageClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
        ApiClient newApiClient = new ApiClient( restTemplate );
        newApiClient.setBasePath( cfg.getSafeStorageBaseUrl() );

        this.fileDownloadApi = new FileDownloadApi( newApiClient );
        this.fileUploadApi =new FileUploadApi( newApiClient );
    }

    public FileDownloadResponse getFile(String fileKey, String xPagopaSafestorageCxId, Boolean metadataOnly) {
        return fileDownloadApi.getFile( fileKey, xPagopaSafestorageCxId, metadataOnly );
    }

    public FileCreationResponse createFile(String xPagopaSafestorageCxId, FileCreationRequest fileCreationRequest) {
        return fileUploadApi.createFile( xPagopaSafestorageCxId, fileCreationRequest );
    }
}
