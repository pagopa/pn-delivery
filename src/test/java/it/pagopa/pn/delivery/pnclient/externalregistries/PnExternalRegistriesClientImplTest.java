package it.pagopa.pn.delivery.pnclient.externalregistries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.PaymentInfoApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.RootSenderIdApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.InfoPaApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroupStatus;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.RootSenderIdResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaInfo;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;

@ContextConfiguration(classes = {PnExternalRegistriesClientImpl.class})
@ExtendWith(SpringExtension.class)
class PnExternalRegistriesClientImplTest {
    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.InternalOnlyApi")
    private InternalOnlyApi internalOnlyApi;

    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.PaymentInfoApi")
    private PaymentInfoApi paymentInfoApi;

    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.RootSenderIdApi")
    private RootSenderIdApi rootSenderIdApi;

    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.InfoPaApi")
    private InfoPaApi infoPaApi;

    @Autowired
    private PnExternalRegistriesClientImpl pnExternalRegistriesClientImpl;

    /**
     * Method under test: {@link PnExternalRegistriesClientImpl#getGroups(String, boolean)}
     */
    @Test
    void testGetGroups() throws RestClientException {
        ArrayList<PaGroup> paGroupList = new ArrayList<>();
        when(internalOnlyApi.getAllGroupsPrivate(Mockito.<String>any(), Mockito.<PaGroupStatus>any()))
                .thenReturn(paGroupList);
        List<PaGroup> actualGroups = pnExternalRegistriesClientImpl.getGroups("42", true);
        assertSame(paGroupList, actualGroups);
        assertTrue(actualGroups.isEmpty());
        verify(internalOnlyApi).getAllGroupsPrivate(Mockito.<String>any(), Mockito.<PaGroupStatus>any());
    }

    /**
     * Method under test: {@link PnExternalRegistriesClientImpl#getPaymentInfo(String, String)}
     */
    @Test
    void testGetPaymentInfo() throws RestClientException {
        PaymentInfo paymentInfo = new PaymentInfo();
        when(paymentInfoApi.getPaymentInfo(Mockito.<String>any(), Mockito.<String>any())).thenReturn(paymentInfo);
        assertSame(paymentInfo, pnExternalRegistriesClientImpl.getPaymentInfo("42", "42"));
        verify(paymentInfoApi).getPaymentInfo(Mockito.<String>any(), Mockito.<String>any());
    }

    /**
     * Method under test: {@link PnExternalRegistriesClientImpl#getGroups(String, boolean)}
     */
    @Test
    void testGetGroups2() throws RestClientException {
        ArrayList<PaGroup> paGroupList = new ArrayList<>();
        when(internalOnlyApi.getAllGroupsPrivate(Mockito.<String>any(), Mockito.<PaGroupStatus>any()))
                .thenReturn(paGroupList);
        List<PaGroup> actualGroups = pnExternalRegistriesClientImpl.getGroups("42", false);
        assertSame(paGroupList, actualGroups);
        assertTrue(actualGroups.isEmpty());
        verify(internalOnlyApi).getAllGroupsPrivate(Mockito.<String>any(), Mockito.<PaGroupStatus>any());
    }

    /**
     * Method under test: {@link PnExternalRegistriesClientImpl#getRootSenderId(String)}
     */
    @Test
    void testGetRootSenderId() {
        RootSenderIdResponse response = new RootSenderIdResponse();
        response.setRootId("rootIdTest");
        when(rootSenderIdApi.getRootSenderIdPrivate(Mockito.anyString())).thenReturn(response);
        String result = pnExternalRegistriesClientImpl.getRootSenderId("senderIdTest");
        assertEquals("rootIdTest", result);
        verify(rootSenderIdApi).getRootSenderIdPrivate(Mockito.anyString());
    }

    /**
     * Method under test: {@link PnExternalRegistriesClientImpl#getOnePa(String)}
     */
    @Test
    void testGetOnePa() {
        PaInfo paInfo = new PaInfo();
        paInfo.setId("paIdTest");
        when(infoPaApi.getOnePa(Mockito.anyString())).thenReturn(paInfo);
        PaInfo result = pnExternalRegistriesClientImpl.getOnePa("paIdTest");
        assertEquals(paInfo, result);
        verify(infoPaApi).getOnePa(Mockito.anyString());
    }
}
