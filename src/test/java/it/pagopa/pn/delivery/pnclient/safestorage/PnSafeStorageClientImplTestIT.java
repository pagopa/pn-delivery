package it.pagopa.pn.delivery.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileDownloadResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery.safe-storage-base-url=http://localhost:9998",
})
class PnSafeStorageClientImplTestIT {

    @Autowired
    private PnSafeStorageClientImpl safeStorageClient;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {

        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }


    @Test
    void getFile() {
        //Given
        String path = "/safe-storage/v1/files/abcd";
        String fileKey = "abcd";
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setChecksum("checksum");
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setDocumentType("PN_NOTIFICATION_ATTACHMENTS");
        fileDownloadResponse.setDocumentStatus("PRELOAD");
        fileDownloadResponse.setKey(fileKey);
        fileDownloadResponse.setVersionId("v1");
        fileDownloadResponse.setDownload(new FileDownloadInfo());


        //When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );
        safeStorageClient.getFile( fileKey, false );

        //Then
        assertDoesNotThrow( () -> {
            safeStorageClient.getFile( fileKey, false );
        });
    }

    @Test
    void createFile() {
        //Given
        String path = "/safe-storage/v1/files";
        String fileKey = "abcd";
        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setStatus("PRELOAD");
        fileCreationRequest.setDocumentType("PAGOPA");
        fileCreationRequest.setContentType("application/pdf");

        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setSecret("secret");
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);
        fileCreationResponse.setKey(fileKey);


        //When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );
        safeStorageClient.createFile ( fileCreationRequest, "base64Sha256");

        //Then
        assertDoesNotThrow( () -> {
            safeStorageClient.createFile ( fileCreationRequest, "base64Sha256");
        });
    }
}
