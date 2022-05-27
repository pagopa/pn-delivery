package it.pagopa.pn.delivery.pnclient.safestorage;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PnSafeStorageClientImplTest {


    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryConfigs cfg;

    private PnSafeStorageClientImpl safeStorageClient;

    @BeforeEach
    void setup() {
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );
        Mockito.when( cfg.getSafeStorageBaseUrl() ).thenReturn( "http://localhost:8080" );
        Mockito.when( cfg.getSafeStorageCxId() ).thenReturn( "pn-delivery-002" );
        this.safeStorageClient = new PnSafeStorageClientImpl( restTemplate, cfg );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getFile() {
        //Given
        String fileKey = "abcd";
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setChecksum("checksum");
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setDocumentType("PN_NOTIFICATION_ATTACHMENTS");
        fileDownloadResponse.setDocumentStatus("PRELOAD");
        fileDownloadResponse.setKey(fileKey);
        fileDownloadResponse.setVersionId("v1");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        ResponseEntity<FileDownloadResponse> response = ResponseEntity.ok( fileDownloadResponse);


        //When
        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn( response );
        FileDownloadResponse result = safeStorageClient.getFile( fileKey, false );

        //Then
        Assertions.assertNotNull( result );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void createFile() {
        //Given
        String fileKey = "abcd";
        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setStatus("PRELOAD");
        fileCreationRequest.setDocumentType("PAGOPA");
        fileCreationRequest.setContentType("application/pdf");

        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setSecret("secret");
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);
        fileCreationResponse.setKey(fileKey);
        ResponseEntity<FileCreationResponse> response = ResponseEntity.ok( fileCreationResponse);


        //When
        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn( response );
        FileCreationResponse result = safeStorageClient.createFile ( fileCreationRequest, "base64Sha256");

        //Then
        Assertions.assertNotNull( result );
    }
}