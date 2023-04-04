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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class PnSafeStorageClientImpl {

    private final FileDownloadApi fileDownloadApi;
    private final FileUploadApi fileUploadApi;
    private final PnDeliveryConfigs cfg;

    public PnSafeStorageClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
        ApiClient newApiClient = new ApiClient( restTemplate );
        newApiClient.setBasePath( cfg.getSafeStorageBaseUrl() );

        this.fileDownloadApi = new FileDownloadApi( newApiClient );
        this.fileUploadApi =new FileUploadApi( newApiClient );
        this.cfg = cfg;
    }

    public FileDownloadResponse getFile(String fileKey, Boolean metadataOnly) {
        return fileDownloadApi.getFile( fileKey, this.cfg.getSafeStorageCxId(), metadataOnly );
    }

    public FileCreationResponse createFile(FileCreationRequest fileCreationRequest, String sha256) {
        log.info("POST LOG: safeStorageCxID {} sha256{} fileCreationRequest{}",this.cfg.getSafeStorageCxId(),sha256,fileCreationRequest.toString());

        return fileUploadApi.createFile(
                this.cfg.getSafeStorageCxId(),
                "SHA-256",
                sha256 ,
                fileCreationRequest);
    }
}
