package it.pagopa.pn.delivery.pnclient.externalregistries;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
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


class PnExternalRegistriesClientImplTest {

    private static final String PA_TAX_ID = "paTaxId";
    private static final String NOTICE_CODE = "noticeCode";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryConfigs cfg;

    private PnExternalRegistriesClientImpl externalRegistriesClient;

    @BeforeEach
    void setup() {
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );
        Mockito.when( cfg.getExternalRegistriesBaseUrl() ).thenReturn( "http://localhost:8080" );
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

}
