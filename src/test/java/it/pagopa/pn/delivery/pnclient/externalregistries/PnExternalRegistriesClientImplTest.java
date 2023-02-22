package it.pagopa.pn.delivery.pnclient.externalregistries;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.externalregistries.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.clients.externalregistries.model.PaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.externalregistries.model.PaymentStatus;
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
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.ArrayList;
import java.util.List;


class PnExternalRegistriesClientImplTest {

    private static final String PA_TAX_ID = "paTaxId";
    private static final String NOTICE_CODE = "noticeCode";
    private static final String SENDER_ID = "senderId";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryConfigs cfg;

    private PnExternalRegistriesClientImpl externalRegistriesClient;

    @BeforeEach
    void setup() {
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );
        Mockito.when( cfg.getExternalRegistriesBaseUrl() ).thenReturn( "http://localhost:8080" );
        Mockito.when((restTemplate.getUriTemplateHandler())).thenReturn(new DefaultUriBuilderFactory());
        this.externalRegistriesClient = new PnExternalRegistriesClientImpl( restTemplate, cfg );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getPaymentInfo() {

        PaymentInfo paymentInfo = new PaymentInfo()
                .amount( 12000 )
                .status( PaymentStatus.REQUIRED )
                .url( "https://api.uat.platform.pagopa.it/checkout/auth/payments/v2" );

        ResponseEntity<PaymentInfo> response = ResponseEntity.ok( paymentInfo );

        // When
        Mockito.when( restTemplate.exchange(  Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn( response );
        PaymentInfo paymentInfoResult = externalRegistriesClient.getPaymentInfo( PA_TAX_ID, NOTICE_CODE );

        // Then
        Assertions.assertNotNull( paymentInfoResult );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getGroupsSuccess() {

        List<PaGroup> res = new ArrayList<>();
        PaGroup dto = new PaGroup();
        dto.setId("123456789");
        dto.setName("amministrazione");
        res.add(dto);
        dto = new PaGroup();
        dto.setId("987654321");
        dto.setName("dirigenza");
        res.add(dto);

        ResponseEntity<List<PaGroup>> response = ResponseEntity.ok( res );

        // When
        Mockito.when( restTemplate.exchange(  Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn( response );
        List<PaGroup> groups = externalRegistriesClient.getGroups( SENDER_ID );

        // Then
        Assertions.assertNotNull( groups );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getGroupsFail() {

        // When
        Mockito.when( restTemplate.exchange(  Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenThrow(PnNotFoundException.class);
        List<PaGroup> groups = externalRegistriesClient.getGroups( SENDER_ID );

        // Then
        Assertions.assertTrue( groups.isEmpty() );
    }

}
