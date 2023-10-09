package it.pagopa.pn.delivery.pnclient.safestorage;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.api.FileUploadApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileDownloadResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;

@ContextConfiguration(classes = {PnSafeStorageClientImpl.class, PnDeliveryConfigs.class})
@ExtendWith(SpringExtension.class)
class PnSafeStorageClientImplTest {
    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.api.FileDownloadApi")
    private FileDownloadApi fileDownloadApi;

    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.api.FileUploadApi")
    private FileUploadApi fileUploadApi;

    @Autowired
    private PnDeliveryConfigs pnDeliveryConfigs;

    @Autowired
    private PnSafeStorageClientImpl pnSafeStorageClientImpl;

    /**
     * Method under test: {@link PnSafeStorageClientImpl#getFile(String, Boolean)}
     */
    @Test
    void testGetFile() throws RestClientException {
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        when(fileDownloadApi.getFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<Boolean>any()))
                .thenReturn(fileDownloadResponse);
        assertSame(fileDownloadResponse, pnSafeStorageClientImpl.getFile("File Key", true));
        verify(fileDownloadApi).getFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<Boolean>any());
    }

    /**
     * Method under test: {@link PnSafeStorageClientImpl#createFile(FileCreationRequest, String)}
     */
    @Test
    void testCreateFile() throws RestClientException {
        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        when(fileUploadApi.createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequest>any())).thenReturn(fileCreationResponse);
        assertSame(fileCreationResponse, pnSafeStorageClientImpl.createFile(new FileCreationRequest(), "Sha256"));
        verify(fileUploadApi).createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequest>any());
    }

    /**
     * Method under test: {@link PnSafeStorageClientImpl#createFile(FileCreationRequest, String)}
     */
    @Test
    @Disabled("TODO: Complete this test")
    void testCreateFile2() throws RestClientException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.NullPointerException: Cannot invoke "it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileCreationRequest.toString()" because "fileCreationRequest" is null
        //       at it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl.createFile(PnSafeStorageClientImpl.java:31)
        //   See https://diff.blue/R013 to resolve this issue.

        when(fileUploadApi.createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequest>any())).thenReturn(new FileCreationResponse());
        pnSafeStorageClientImpl.createFile(null, "Sha256");
    }

    /**
     * Method under test: {@link PnSafeStorageClientImpl#createFile(FileCreationRequest, String)}
     */
    @Test
    void testCreateFile3() throws RestClientException {
        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        when(fileUploadApi.createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequest>any())).thenReturn(fileCreationResponse);
        assertSame(fileCreationResponse, pnSafeStorageClientImpl.createFile(mock(FileCreationRequest.class), "Sha256"));
        verify(fileUploadApi).createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequest>any());
    }
}

