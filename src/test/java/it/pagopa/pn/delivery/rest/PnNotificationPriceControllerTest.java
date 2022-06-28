package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPriceResponse;
import it.pagopa.pn.delivery.svc.NotificationPriceService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
@WebFluxTest(PnNotificationPriceController.class)
class PnNotificationPriceControllerTest {

    private static final String PA_TAX_ID = "77777777777";
    private static final String NOTICE_CODE = "302000100000019421";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private NotificationPriceService service;

    @MockBean
    private PnDeliveryConfigs cfg;

    @Test
    void getPriceSuccess() {
        //Given
        NotificationPriceResponse priceResponse = NotificationPriceResponse.builder()
                .iun( "iun" )
                .effectiveDate( Date.from( Instant.now() ) )
                .amount( "2000" )
                .build();

        //When
        Mockito.when( service.getNotificationPrice( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( priceResponse );

        webTestClient.get()
                .uri( "/delivery/price/{paTaxId}/{noticeCode}"
                        .replace( "{paTaxId}", PA_TAX_ID )
                        .replace( "{noticeCode}", NOTICE_CODE ))
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody( NotificationPriceResponse.class );


        //Then
        Mockito.verify( service ).getNotificationPrice( PA_TAX_ID, NOTICE_CODE );
    }

    @Test
    void getPriceFailure() {

        Mockito.when( service.getNotificationPrice( Mockito.anyString(), Mockito.anyString() ) ).thenThrow(PnNotFoundException.class);

        webTestClient.get()
                .uri( "/delivery/price/{paTaxId}/{noticeCode}"
                        .replace( "{paTaxId}", PA_TAX_ID )
                        .replace( "{noticeCode}", NOTICE_CODE ))
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isNotFound();

    }

}
