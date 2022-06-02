package it.pagopa.pn.delivery.middleware.notificationdao;




import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class NotificationMetadataEntityDaoDynamoTest {
    private NotificationMetadataEntityDao metadataEntityDao;


    @BeforeEach
    void setup() {
        metadataEntityDao = new NotificationMetadataEntityDaoDynamoTest.MetadataEntityDaoMock();
    }

    @Test
    void InsertMetadataEntitySuccess() throws IdConflictException {
        //Given
        NotificationMetadataEntity entityToInsert = NotificationMetadataEntity.builder()
                .notificationGroup("Notification_Group")
                .notificationStatus( NotificationStatus.ACCEPTED.toString() )
                .iun_recipientId( "IUN##RecipientId" )
                .recipientId_creationMonth( "RecipientId##creationMonth" )
                .recipientIds( Collections.singletonList( "RecipientId" ))
                .recipientOne( true )
                .senderId( "SenderId" )
                .senderId_creationMonth( "SenderId##CreationMonth" )
                .senderId_recipientId( "SenderId##RecipientId" )
                .sentAt( Instant.parse( "2022-04-06T17:48:00Z" ) )
                .tableRow( Map.ofEntries(
                        Map.entry( "iun", "IUN" ),
                        Map.entry( "recipientsIds", Collections.singletonList( "RecipientId" ).toString() ),
                        Map.entry( "paNotificationId", "PaNotificationId" ),
                        Map.entry( "subject", "Subject"  ) )
                )
                .recipientId( "RecipientId" )
                .build();

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(true)
                .senderReceiverId("SenderId")
                .startDate( Instant.parse("2022-04-01T17:48:00Z") )
                .endDate( Instant.parse("2022-04-30T17:48:00Z") )
                .size(10)
                .nextPagesKey(null)
                .build();

        //When
        metadataEntityDao.putIfAbsent( entityToInsert );

        //Then
        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = metadataEntityDao.searchForOneMonth(
                searchDto,
                "senderId",
                "SenderId##CreationMonth",
                10,
                null);

        Assertions.assertNotNull( result );
    }

    private static class MetadataEntityDaoMock implements NotificationMetadataEntityDao {

        private EntityToDtoNotificationMetadataMapper entityToDto = new EntityToDtoNotificationMetadataMapper();

        private final Map<Key, NotificationMetadataEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void put(NotificationMetadataEntity notificationMetadataEntity) {

        }

        @Override
        public void putIfAbsent(NotificationMetadataEntity notificationMetadataEntity) throws IdConflictException {
            Key key = Key.builder()
                    .partitionValue( notificationMetadataEntity.getIun_recipientId() )
                    .sortValue( notificationMetadataEntity.getSentAt().toString() )
                    .build();
            storage.put( key, notificationMetadataEntity );
        }

        @Override
        public Optional<NotificationMetadataEntity> get(Key key) {
            return Optional.of( storage.get(key) );
        }

        @Override
        public void delete(Key key) {

        }

        @Override
        public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchForOneMonth(InputSearchNotificationDto inputSearchNotificationDto, String indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {
            Key key = Key.builder()
                    .partitionValue( "IUN##RecipientId" )
                    .sortValue( "2022-04-06T17:48:00Z" )
                    .build();
            NotificationMetadataEntity getResult = storage.get( key );

            return ResultPaginationDto.<NotificationSearchRow, PnLastEvaluatedKey>builder()
                    .moreResult( false )
                    .resultsPage( Collections.singletonList( entityToDto.entity2Dto( getResult ) ) )
                    .build();
        }
    }

}