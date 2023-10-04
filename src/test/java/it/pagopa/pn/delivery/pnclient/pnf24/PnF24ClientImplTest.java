package it.pagopa.pn.delivery.pnclient.pnf24;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.api.F24ControllerApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.model.F24Response;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.model.RequestAccepted;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.model.SaveF24Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;

@ContextConfiguration(classes = {PnF24ClientImpl.class})
@ExtendWith(SpringExtension.class)
class PnF24ClientImplTest {
    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.api.F24ControllerApi")
    private F24ControllerApi f24ControllerApi;

    @Autowired
    private PnF24ClientImpl pnF24ClientImpl;

    /**
     * Method under test: {@link PnF24ClientImpl#saveMetadata(String, String, SaveF24Request)}
     */
    @Test
    void testSaveMetadata() throws RestClientException {
        RequestAccepted requestAccepted = new RequestAccepted();
        when(f24ControllerApi.saveMetadata(Mockito.<String>any(), Mockito.<String>any(), Mockito.<SaveF24Request>any()))
                .thenReturn(requestAccepted);
        assertSame(requestAccepted, pnF24ClientImpl.saveMetadata("42", "42", new SaveF24Request()));
        verify(f24ControllerApi).saveMetadata(Mockito.<String>any(), Mockito.<String>any(), Mockito.<SaveF24Request>any());
    }

    @Test
    void testGeneratePDF() throws RestClientException {
        F24Response requestAccepted = new F24Response();
        when(f24ControllerApi.generatePDF(anyString(),anyString(),anyList(),anyInt()))
                .thenReturn(requestAccepted);
        assertSame(requestAccepted, pnF24ClientImpl.generatePDF("42", "42", new ArrayList<>(), 0));
        verify(f24ControllerApi).generatePDF(anyString(),anyString(),anyList(),anyInt());
    }
}

