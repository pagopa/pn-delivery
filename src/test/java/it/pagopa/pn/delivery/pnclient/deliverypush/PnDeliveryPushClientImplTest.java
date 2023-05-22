package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.*;
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

import java.time.OffsetDateTime;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class PnDeliveryPushClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryConfigs cfg;

    private PnDeliveryPushClientImpl deliveryPushClient;

    @BeforeEach
    void setup() {
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );
        Mockito.when( cfg.getDeliveryPushBaseUrl() ).thenReturn( "http://localhost:8080" );
//        Mockito.when((restTemplate.getUriTemplateHandler())).thenReturn(new DefaultUriBuilderFactory());
        this.deliveryPushClient = new PnDeliveryPushClientImpl( restTemplate, cfg );
    }

    @Test
    void getTimelineAndStatusHistory() {
        // Given
        NotificationHistoryResponse historyResponse = new NotificationHistoryResponse()
                .notificationStatusHistory( Collections.singletonList(new NotificationStatusHistoryElement()
                        .status( NotificationStatus.ACCEPTED )
                        .activeFrom( OffsetDateTime.now() )
                        .relatedTimelineElements( Collections.singletonList( "elementId" ) )) )
                .notificationStatus( NotificationStatus.ACCEPTED )
                .timeline( Collections.singletonList(new TimelineElement()
                        .timestamp( OffsetDateTime.now() )
                        .category( TimelineElementCategory.REQUEST_ACCEPTED )
                        .elementId( "elementId" )
                        .legalFactsIds( Collections.singletonList( new LegalFactsId()
                                .category( LegalFactCategory.SENDER_ACK )
                                .key( "key" )
                        ))
                ));
        ResponseEntity<NotificationHistoryResponse> response = ResponseEntity.ok( historyResponse );

        // When
        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);
        NotificationHistoryResponse notificationHistoryResponse = deliveryPushClient.getTimelineAndStatusHistory( "iun", 1, OffsetDateTime.now() );

        // Then
        Assertions.assertNotNull( notificationHistoryResponse );
    }

}
