package it.pagopa.pn.delivery.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.asseverationeventsproducer.SqsAsseverationEventsProducer;
import it.pagopa.pn.delivery.models.AsseverationEvent;
import it.pagopa.pn.delivery.models.InternalAsseverationEvent;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

class AsseverationEventsProducerTest {

    private AsseverationEventsProducer asseverationEventsProducer;

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
        asseverationEventsProducer = new SqsAsseverationEventsProducer(sqsClient, objectMapper, cfg);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void buildAsseverationEvent() {
        Instant now = Instant.now();
        InternalAsseverationEvent internalAsseverationEvent = InternalAsseverationEvent.builder()
                .iun( "IUN" )
                .noticeCode( "noticeCode" )
                .creditorTaxId( "creditorTaxId" )
                .senderPaId( "senderPaId" )
                .recipientIdx( 0 )
                .debtorPosUpdateDate( now.toString() )
                .recordCreationDate( now.toString() )
                .notificationSentAt( "2023-01-18T12:34:00.000Z" )
                .version( 1 )
                .moreFields( null )
                .build();
        AsseverationEvent asseverationEvent = asseverationEventsProducer.buildAsseverationEvent(internalAsseverationEvent);

        Assertions.assertNotNull( asseverationEvent );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAsseverationEvent() {
        Instant now = Instant.now();
        InternalAsseverationEvent internalAsseverationEvent = InternalAsseverationEvent.builder()
                .iun( "IUN" )
                .noticeCode( "noticeCode" )
                .creditorTaxId( "creditorTaxId" )
                .senderPaId( "senderPaId" )
                .recipientIdx( 0 )
                .debtorPosUpdateDate( now.toString() )
                .recordCreationDate( now.toString() )
                .notificationSentAt( "2023-01-18T12:34:00.000Z" )
                .version( 1 )
                .moreFields( null )
                .build();
        assertThrows(NullPointerException.class, () -> asseverationEventsProducer.sendAsseverationEvent(internalAsseverationEvent));
    }

}