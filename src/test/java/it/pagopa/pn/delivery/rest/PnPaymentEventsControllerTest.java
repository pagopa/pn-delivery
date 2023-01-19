package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventF24;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventPagoPa;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequestF24;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequestPagoPa;
import it.pagopa.pn.delivery.svc.PaymentEventsService;
import it.pagopa.pn.delivery.utils.PnDeliveryRestConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collections;

@WebFluxTest(PnPaymentEventsController.class)
class PnPaymentEventsControllerTest {

    private static final String CX_ID_PA_ID = "paId";
    public static final String CX_TYPE_PA = "PA";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    PaymentEventsService paymentEventsService;


    @Test
    void paymentEventsPagoPaPostSuccess() {

        PaymentEventsRequestPagoPa paymentEventsRequestPagoPa = PaymentEventsRequestPagoPa.builder()
                .events( Collections.singletonList( PaymentEventPagoPa.builder()
                                .paymentDate( OffsetDateTime.parse( "2023-01-16T15:30:00Z" ) )
                                .creditorTaxId( "77777777777" )
                                .noticeCode( "123456789123456789" )
                        .build() )
                )
                .build();


        webTestClient.post()
                .uri("/delivery/events/payment/pagopa")
                .contentType(MediaType.APPLICATION_JSON)
                //.accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(paymentEventsRequestPagoPa), PaymentEventsRequestPagoPa.class)
                .header(PnDeliveryRestConstants.CX_ID_HEADER, CX_ID_PA_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
                //.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
                .exchange()
                .expectStatus().isNoContent();

        Mockito.verify( paymentEventsService ).handlePaymentEventsPagoPa( CX_TYPE_PA, CX_ID_PA_ID, paymentEventsRequestPagoPa );

    }

    @Test
    void paymentEventsPagoPaPostFailure() {

        PaymentEventsRequestPagoPa paymentEventsRequestPagoPa = PaymentEventsRequestPagoPa.builder()
                .events( Collections.singletonList( PaymentEventPagoPa.builder()
                        .paymentDate( OffsetDateTime.parse( "2023-01-16T15:30:00Z" ) )
                        .creditorTaxId( "77777777777" )
                        .noticeCode( "123456789123456789" )
                        .build() )
                )
                .build();

        Mockito.doThrow( new PnRuntimeException( "test", "description", 400, "errorCode", "element", "detail" ) )
                .when( paymentEventsService )
                .handlePaymentEventsPagoPa( Mockito.anyString(), Mockito.anyString(), Mockito.any( PaymentEventsRequestPagoPa.class ) );

        webTestClient.post()
                .uri("/delivery/events/payment/pagopa")
                .contentType(MediaType.APPLICATION_JSON)
                //.accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(paymentEventsRequestPagoPa), PaymentEventsRequestPagoPa.class)
                .header(PnDeliveryRestConstants.CX_ID_HEADER, CX_ID_PA_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
                //.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
                .exchange()
                .expectStatus().isBadRequest();

    }

    @Test
    void paymentEventsF24PostSuccess() {

        PaymentEventsRequestF24 paymentEventsRequestF24 = PaymentEventsRequestF24.builder()
                .events( Collections.singletonList( PaymentEventF24.builder()
                        .paymentDate( OffsetDateTime.parse( "2023-01-16T15:30:00Z" ) )
                        .iun( "IUN" )
                        .recipientTaxId( "12345678901" )
                        .recipientType( "PG" )
                        .build() )
                )
                .build();


        webTestClient.post()
                .uri("/delivery/events/payment/f24")
                .contentType(MediaType.APPLICATION_JSON)
                //.accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(paymentEventsRequestF24), PaymentEventsRequestF24.class)
                .header(PnDeliveryRestConstants.CX_ID_HEADER, CX_ID_PA_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
                //.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
                .exchange()
                .expectStatus().isNoContent();

        Mockito.verify( paymentEventsService ).handlePaymentEventsF24( CX_TYPE_PA, CX_ID_PA_ID, paymentEventsRequestF24 );

    }

    @Test
    void paymentEventsF24PostFailure() {

        PaymentEventsRequestF24 paymentEventsRequestF24 = PaymentEventsRequestF24.builder()
                .events( Collections.singletonList( PaymentEventF24.builder()
                        .paymentDate( OffsetDateTime.parse( "2023-01-16T15:30:00Z" ) )
                        .iun( "IUN" )
                        .recipientTaxId( "12345678901" )
                        .recipientType( "PG" )
                        .build() )
                )
                .build();

        Mockito.doThrow( new PnRuntimeException( "test", "description", 400, "errorCode", "element", "detail" ) )
                .when( paymentEventsService )
                .handlePaymentEventsF24( Mockito.anyString(), Mockito.anyString(), Mockito.any( PaymentEventsRequestF24.class ) );


        webTestClient.post()
                .uri("/delivery/events/payment/f24")
                .contentType(MediaType.APPLICATION_JSON)
                //.accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(paymentEventsRequestF24), PaymentEventsRequestF24.class)
                .header(PnDeliveryRestConstants.CX_ID_HEADER, CX_ID_PA_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
                //.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
                .exchange()
                .expectStatus().isBadRequest();

    }

}
