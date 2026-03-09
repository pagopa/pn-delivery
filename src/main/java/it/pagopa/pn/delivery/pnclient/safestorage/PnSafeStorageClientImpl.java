package it.pagopa.pn.delivery.pnclient.safestorage;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.api.FileUploadApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileDownloadResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@CustomLog
@RequiredArgsConstructor
public class PnSafeStorageClientImpl {

    private final FileDownloadApi fileDownloadApi;
    private final FileUploadApi fileUploadApi;
    private final PnDeliveryConfigs cfg;


    public FileDownloadResponse getFile(String fileKey, Boolean metadataOnly, Boolean tags) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, "getFile");
        return fileDownloadApi.getFile( fileKey, this.cfg.getSafeStorageCxId(), metadataOnly, tags);
    }

    public FileCreationResponse createFile(FileCreationRequest fileCreationRequest, String sha256) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, "createFile");
        log.info("POST LOG: safeStorageCxID {} sha256 {} fileCreationRequest{}",this.cfg.getSafeStorageCxId(),sha256,fileCreationRequest.toString());

        return fileUploadApi.createFile(
                this.cfg.getSafeStorageCxId(),
                sha256,
                "SHA-256",
                fileCreationRequest);
    }
}
