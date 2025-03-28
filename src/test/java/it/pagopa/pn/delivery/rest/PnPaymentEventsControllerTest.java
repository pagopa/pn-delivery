package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventF24;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventPagoPa;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequestF24;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequestPagoPa;
import it.pagopa.pn.delivery.svc.PaymentEventsService;
import it.pagopa.pn.delivery.utils.PaymentEventsLogUtil;
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

import static org.mockito.ArgumentMatchers.isNull;

@WebFluxTest(PnPaymentEventsController.class)
class PnPaymentEventsControllerTest {
    private static final String IUN = "AAAA-AAAA-AAAA-202301-C-1";
    private static final String TAX_ID = "CSRGGL44L13H501E";

    private static final String CX_ID_PA_ID = "paId";
    public static final String CX_TYPE_PA = "PA";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    PaymentEventsLogUtil paymentEventsLogUtil;

    @MockBean
    PaymentEventsService paymentEventsService;


    @Test
    void paymentEventsPagoPaPostSuccess() {

        PaymentEventsRequestPagoPa paymentEventsRequestPagoPa = PaymentEventsRequestPagoPa.builder()
                .events( Collections.singletonList( PaymentEventPagoPa.builder()
                                .paymentDate(  "2023-12-16T15:30:00.123Z"  )
                                .creditorTaxId( "77777777777" )
                                .noticeCode( "123456789123456789" )
                                .amount( 2000 )
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

        Mockito.verify( paymentEventsService ).handlePaymentEventsPagoPa( CX_TYPE_PA, CX_ID_PA_ID, null, paymentEventsRequestPagoPa );

    }

    @Test
    void paymentEventsPagoPaPostFailure() {

        PaymentEventsRequestPagoPa paymentEventsRequestPagoPa = PaymentEventsRequestPagoPa.builder()
                .events( Collections.singletonList( PaymentEventPagoPa.builder()
                        .paymentDate( "2023-01-16T15:30:00Z" )
                        .creditorTaxId( "77777777777" )
                        .noticeCode( "123456789123456789" )
                        .build() )
                )
                .build();

        Mockito.doThrow( new PnRuntimeException( "test", "description", 400, "errorCode", "element", "detail" ) )
                .when( paymentEventsService )
                .handlePaymentEventsPagoPa( Mockito.anyString(), Mockito.anyString(), isNull(), Mockito.any( PaymentEventsRequestPagoPa.class ) );

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

}
