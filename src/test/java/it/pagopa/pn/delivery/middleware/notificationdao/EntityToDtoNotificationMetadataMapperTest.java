package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EntityToDtoNotificationMetadataMapperTest {

    public static final String RECIPIENT_ID = "PF-4fc75df3-0913-407e-bdaa-e50329708b7d";
    public static final String NOTIFICATION_GROUP = "";
    public static final String NOTIFICATION_STATUS = "DELIVERING";
    public static final String IUN_RECIPIENT_ID = "TGWR-ZJQN-JMAR-202209-A-1##PF-4fc75df3-0913-407e-bdaa-e50329708b7d";
    public static final String RECIPIENT_ID_CREATION_MONTH = "PF-4fc75df3-0913-407e-bdaa-e50329708b7d##202209";
    public static final String SENDER_ID_RECIPIENT_ID = "026e8c72-7944-4dcd-8668-f596447fec6d##PF-4fc75df3-0913-407e-bdaa-e50329708b7d";
    public static final String IUN = "TGWR-ZJQN-JMAR-202209-A-1";
    public static final String PA_PROTOCOL_NUMBER = "1662403658809";
    public static final String SENDER_DENOMINATION = "comune di milano";
    public static final String SUBJECT = "invio notifica con cucumber 05-09-2022 18:47:38";
    public static final String SENT_AT = "2022-09-05T18:47:39.267123Z";
    public static final Map<String,String> TABLE_ROW = Map.of(
            "acceptedAt", SENT_AT,
            "iun", IUN,
            "paProtocolNumber", PA_PROTOCOL_NUMBER,
            "senderDenomination", SENDER_DENOMINATION,
            "subject", SUBJECT
    );


    private EntityToDtoNotificationMetadataMapper mapper;

    @BeforeEach
    void setup() {
        this.mapper = new EntityToDtoNotificationMetadataMapper();
    }

    @Test
    void mapperSuccess() {
        // Given
        NotificationMetadataEntity metadataEntity = new NotificationMetadataEntity();
        metadataEntity.setRecipientIds( List.of( RECIPIENT_ID ));
        metadataEntity.setRecipientId( RECIPIENT_ID );
        metadataEntity.setNotificationGroup( NOTIFICATION_GROUP );
        metadataEntity.setNotificationStatus( NOTIFICATION_STATUS );
        metadataEntity.setIunRecipientId( IUN_RECIPIENT_ID );
        metadataEntity.setRecipientIdCreationMonth( RECIPIENT_ID_CREATION_MONTH );
        metadataEntity.setSenderIdRecipientId( SENDER_ID_RECIPIENT_ID );
        metadataEntity.setRecipientOne( true );
        metadataEntity.setSentAt( Instant.parse(SENT_AT) );
        metadataEntity.setTableRow( TABLE_ROW );

        // When
        NotificationSearchRow result = mapper.entity2Dto( metadataEntity );

        // Then
        assertNotNull( result );
        assertEquals( IUN, result.getIun());
        assertEquals( PA_PROTOCOL_NUMBER, result.getPaProtocolNumber());
        assertEquals( SENDER_DENOMINATION, result.getSender());
        assertEquals( SENT_AT, result.getSentAt().toString() );
        assertEquals( SUBJECT, result.getSubject() );
        assertEquals( NOTIFICATION_STATUS, result.getNotificationStatus().getValue() );
        assertEquals( NOTIFICATION_GROUP, result.getGroup() );
    }

}
