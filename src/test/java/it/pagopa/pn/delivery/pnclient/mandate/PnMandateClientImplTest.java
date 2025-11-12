package it.pagopa.pn.delivery.pnclient.mandate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.api.MandatePrivateServiceApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.api.MandatePrivateServiceV2Api;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.DelegateType;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.MandateByDelegatorRequestDto;

import java.time.OffsetDateTime;
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

@ContextConfiguration(classes = {PnMandateClientImpl.class})
@ExtendWith(SpringExtension.class)
class PnMandateClientImplTest {
    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.api.MandatePrivateServiceApi")
    private MandatePrivateServiceApi mandatePrivateServiceApi;

    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.api.MandatePrivateServiceV2Api")
    private MandatePrivateServiceV2Api mandatePrivateServiceV2Api;

    @Autowired
    private PnMandateClientImpl pnMandateClientImpl;

    /**
     * Method under test: {@link PnMandateClientImpl#listMandatesByDelegate(String, String, CxTypeAuthFleet, List)}
     */
    @Test
    void testListMandatesByDelegate() throws RestClientException {
        ArrayList<InternalMandateDto> internalMandateDtoList = new ArrayList<>();
        when(mandatePrivateServiceApi.listMandatesByDelegate(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any())).thenReturn(internalMandateDtoList);
        List<InternalMandateDto> actualListMandatesByDelegateResult = pnMandateClientImpl
                .listMandatesByDelegate("Delegated", "2020-03-01", CxTypeAuthFleet.PG, new ArrayList<>());
        assertSame(internalMandateDtoList, actualListMandatesByDelegateResult);
        assertTrue(actualListMandatesByDelegateResult.isEmpty());
        verify(mandatePrivateServiceApi).listMandatesByDelegate(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any());
    }

    /**
     * Method under test: {@link PnMandateClientImpl#listMandatesByDelegate(String, String, CxTypeAuthFleet, List)}
     */
    @Test
    void testListMandatesByDelegate2() throws RestClientException {
        ArrayList<InternalMandateDto> internalMandateDtoList = new ArrayList<>();
        when(mandatePrivateServiceApi.listMandatesByDelegate(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any())).thenReturn(internalMandateDtoList);
        List<InternalMandateDto> actualListMandatesByDelegateResult = pnMandateClientImpl
                .listMandatesByDelegate("Delegated", "2020-03-01", CxTypeAuthFleet.PF, new ArrayList<>());
        assertSame(internalMandateDtoList, actualListMandatesByDelegateResult);
        assertTrue(actualListMandatesByDelegateResult.isEmpty());
        verify(mandatePrivateServiceApi).listMandatesByDelegate(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any());
    }

    /**
     * Method under test: {@link PnMandateClientImpl#listMandatesByDelegate(String, String, CxTypeAuthFleet, List)}
     */
    @Test
    void testListMandatesByDelegate3() throws RestClientException {
        ArrayList<InternalMandateDto> internalMandateDtoList = new ArrayList<>();
        when(mandatePrivateServiceApi.listMandatesByDelegate(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any())).thenReturn(internalMandateDtoList);
        List<InternalMandateDto> actualListMandatesByDelegateResult = pnMandateClientImpl
                .listMandatesByDelegate("Delegated", "2020-03-01", CxTypeAuthFleet.PA, new ArrayList<>());
        assertSame(internalMandateDtoList, actualListMandatesByDelegateResult);
        assertTrue(actualListMandatesByDelegateResult.isEmpty());
        verify(mandatePrivateServiceApi).listMandatesByDelegate(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any());
    }

    /**
     * Method under test: {@link PnMandateClientImpl#listMandatesByDelegator(String, String, CxTypeAuthFleet, List, String, DelegateType)}
     */
    @Test
    void testListMandatesByDelegator() throws RestClientException {
        ArrayList<InternalMandateDto> internalMandateDtoList = new ArrayList<>();
        when(mandatePrivateServiceApi.listMandatesByDelegator(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any(), Mockito.<String>any(), Mockito.<DelegateType>any()))
                .thenReturn(internalMandateDtoList);
        List<InternalMandateDto> actualListMandatesByDelegatorResult = pnMandateClientImpl.listMandatesByDelegator(
                "Delegator", "2020-03-01", CxTypeAuthFleet.PG, new ArrayList<>(), "Cx Role", DelegateType.PG);
        assertSame(internalMandateDtoList, actualListMandatesByDelegatorResult);
        assertTrue(actualListMandatesByDelegatorResult.isEmpty());
        verify(mandatePrivateServiceApi).listMandatesByDelegator(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any(), Mockito.<String>any(), Mockito.<DelegateType>any());
    }

    /**
     * Method under test: {@link PnMandateClientImpl#listMandatesByDelegator(String, String, CxTypeAuthFleet, List, String, DelegateType)}
     */
    @Test
    void testListMandatesByDelegator2() throws RestClientException {
        ArrayList<InternalMandateDto> internalMandateDtoList = new ArrayList<>();
        when(mandatePrivateServiceApi.listMandatesByDelegator(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any(), Mockito.<String>any(), Mockito.<DelegateType>any()))
                .thenReturn(internalMandateDtoList);
        List<InternalMandateDto> actualListMandatesByDelegatorResult = pnMandateClientImpl.listMandatesByDelegator(
                "Delegator", "2020-03-01", CxTypeAuthFleet.PF, new ArrayList<>(), "Cx Role", DelegateType.PG);
        assertSame(internalMandateDtoList, actualListMandatesByDelegatorResult);
        assertTrue(actualListMandatesByDelegatorResult.isEmpty());
        verify(mandatePrivateServiceApi).listMandatesByDelegator(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any(), Mockito.<String>any(), Mockito.<DelegateType>any());
    }

    /**
     * Method under test: {@link PnMandateClientImpl#listMandatesByDelegator(String, String, CxTypeAuthFleet, List, String, DelegateType)}
     */
    @Test
    void testListMandatesByDelegator3() throws RestClientException {
        ArrayList<InternalMandateDto> internalMandateDtoList = new ArrayList<>();
        when(mandatePrivateServiceApi.listMandatesByDelegator(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any(), Mockito.<String>any(), Mockito.<DelegateType>any()))
                .thenReturn(internalMandateDtoList);
        List<InternalMandateDto> actualListMandatesByDelegatorResult = pnMandateClientImpl.listMandatesByDelegator(
                "Delegator", "2020-03-01", CxTypeAuthFleet.PA, new ArrayList<>(), "Cx Role", DelegateType.PG);
        assertSame(internalMandateDtoList, actualListMandatesByDelegatorResult);
        assertTrue(actualListMandatesByDelegatorResult.isEmpty());
        verify(mandatePrivateServiceApi).listMandatesByDelegator(Mockito.<String>any(), Mockito.<CxTypeAuthFleet>any(),
                Mockito.<String>any(), Mockito.<List<String>>any(), Mockito.<String>any(), Mockito.<DelegateType>any());
    }

    /**
     * Method under test: {@link PnMandateClientImpl#listMandatesByDelegators(DelegateType, List, List)}
     */
    @Test
    void testListMandatesByDelegators() throws RestClientException {
        ArrayList<InternalMandateDto> internalMandateDtoList = new ArrayList<>();
        when(mandatePrivateServiceApi.listMandatesByDelegators(Mockito.<DelegateType>any(), Mockito.<List<String>>any(),
                Mockito.<List<MandateByDelegatorRequestDto>>any())).thenReturn(internalMandateDtoList);
        ArrayList<String> cxGroups = new ArrayList<>();
        List<InternalMandateDto> actualListMandatesByDelegatorsResult = pnMandateClientImpl
                .listMandatesByDelegators(DelegateType.PG, cxGroups, new ArrayList<>());
        assertSame(internalMandateDtoList, actualListMandatesByDelegatorsResult);
        assertTrue(actualListMandatesByDelegatorsResult.isEmpty());
        verify(mandatePrivateServiceApi).listMandatesByDelegators(Mockito.<DelegateType>any(),
                Mockito.<List<String>>any(), Mockito.<List<MandateByDelegatorRequestDto>>any());
    }

    /**
     * Method under test: {@link PnMandateClientImpl#listMandatesByDelegators(DelegateType, List, List)}
     */
    @Test
    void testListMandatesByDelegators2() throws RestClientException {
        ArrayList<InternalMandateDto> internalMandateDtoList = new ArrayList<>();
        when(mandatePrivateServiceApi.listMandatesByDelegators(Mockito.<DelegateType>any(), Mockito.<List<String>>any(),
                Mockito.<List<MandateByDelegatorRequestDto>>any())).thenReturn(internalMandateDtoList);
        ArrayList<String> cxGroups = new ArrayList<>();
        List<InternalMandateDto> actualListMandatesByDelegatorsResult = pnMandateClientImpl
                .listMandatesByDelegators(DelegateType.PF, cxGroups, new ArrayList<>());
        assertSame(internalMandateDtoList, actualListMandatesByDelegatorsResult);
        assertTrue(actualListMandatesByDelegatorsResult.isEmpty());
        verify(mandatePrivateServiceApi).listMandatesByDelegators(Mockito.<DelegateType>any(),
                Mockito.<List<String>>any(), Mockito.<List<MandateByDelegatorRequestDto>>any());
    }

    /**
     * Method under test: {@link PnMandateClientImpl#listMandatesByDelegateV2(String, String, CxTypeAuthFleet, List, OffsetDateTime, String, String)}
     */
    @Test
    void listMandatesByDelegateV2() {
        ArrayList<InternalMandateDto> internalMandateDtoList = new ArrayList<>();
        when(mandatePrivateServiceV2Api.listMandatesByDelegateV2(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(internalMandateDtoList);
        ArrayList<String> cxGroups = new ArrayList<>();
        List<InternalMandateDto> actualListMandatesByDelegatorsResult = pnMandateClientImpl
                .listMandatesByDelegateV2("Delegated", "testMandateId", CxTypeAuthFleet.PF, cxGroups, OffsetDateTime.now(), "iunTest", "rootSenderIdTest");
        assertSame(internalMandateDtoList, actualListMandatesByDelegatorsResult);
        assertTrue(actualListMandatesByDelegatorsResult.isEmpty());
        verify(mandatePrivateServiceV2Api).listMandatesByDelegateV2(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
}

