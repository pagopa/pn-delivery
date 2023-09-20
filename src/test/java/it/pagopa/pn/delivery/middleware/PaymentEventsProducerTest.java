package it.pagopa.pn.delivery.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.paymenteventsproducer.SqsPaymentEventsProducer;
import it.pagopa.pn.delivery.models.InternalPaymentEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentEventsProducerTest {

    private PaymentEventsProducer paymentEventsProducer;

    @Mock
    private SqsClient sqsClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PnDeliveryConfigs cfg;

    @BeforeEach
    void setup() {
        Mockito.when( cfg.getTopics() ).thenReturn (new PnDeliveryConfigs.Topics());

        software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse response = GetQueueUrlResponse.builder()
                .queueUrl( "queueUrl" )
                .build();

        Mockito.when( sqsClient.getQueueUrl( Mockito.any( software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest.class ))).thenReturn( response );
        paymentEventsProducer = new SqsPaymentEventsProducer( sqsClient, objectMapper, cfg );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendPaymentEvents() {
        // Given
        List<InternalPaymentEvent> internalPaymentEvents = List.of(InternalPaymentEvent.builder()
                .iun("IUN")
                .paymentDate(Instant.parse("2023-01-18T12:34:00Z"))
                .paymentType(PnDeliveryPaymentEvent.PaymentType.PAGOPA)
                .recipientIdx(0)
                .recipientType(PnDeliveryPaymentEvent.RecipientType.PF)
                .creditorTaxId("creditorTaxId")
                .noticeCode("noticeCode")
                .build()
        );

        assertThrows(NullPointerException.class, () -> paymentEventsProducer.sendPaymentEvents( internalPaymentEvents) );


    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void buildPaymentEvents() {
        // Given
        List<InternalPaymentEvent> internalPaymentEvents = List.of(InternalPaymentEvent.builder()
                .iun("IUN")
                .paymentDate(Instant.parse("2023-01-18T12:34:00Z"))
                .paymentType(PnDeliveryPaymentEvent.PaymentType.PAGOPA)
                .recipientIdx(0)
                .recipientType(PnDeliveryPaymentEvent.RecipientType.PF)
                .creditorTaxId("creditorTaxId")
                .noticeCode("noticeCode")
                .build()
        );

        // When
        List<PnDeliveryPaymentEvent> results = paymentEventsProducer.buildPaymentEvents( internalPaymentEvents );

        // Then
        Assertions.assertNotNull( results );

    }
}
