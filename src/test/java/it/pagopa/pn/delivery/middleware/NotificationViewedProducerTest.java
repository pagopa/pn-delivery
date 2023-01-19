package it.pagopa.pn.delivery.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.newnotificationproducer.SqsNotificationViewedProducer;
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

class NotificationViewedProducerTest {

    private NotificationViewedProducer notificationViewedProducer;

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
        notificationViewedProducer = new SqsNotificationViewedProducer( sqsClient, objectMapper, cfg );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void buildNotificationViewed() {
        // Given
        String iun = "IUN";
        Instant when = Instant.parse( "2023-01-19T12:01:12Z" ) ;
        int recipientIndex = 0;

        NotificationViewDelegateInfo delegateInfo = NotificationViewDelegateInfo.builder()
                .internalId( "internalId" )
                .operatorUuid( "operatorUid" )
                .delegateType( NotificationViewDelegateInfo.DelegateType.PF )
                .mandateId( "mandateId" )
                .build();

        // When
        PnDeliveryNotificationViewedEvent result = notificationViewedProducer.buildNotificationViewed( iun, when, recipientIndex, delegateInfo );

        // Then
        Assertions.assertNotNull( result );

    }
}
