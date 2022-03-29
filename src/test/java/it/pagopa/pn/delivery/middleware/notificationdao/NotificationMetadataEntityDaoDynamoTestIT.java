package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.middleware.model.notification.NotificationMetadataEntity;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        NotificationMetadataEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "aws.region-code=us-east-1",
        "aws.profile-name=default",
        "aws.endpoint-url=http://localhost:4566",
})
@SpringBootTest
class NotificationMetadataEntityDaoDynamoTestIT {

    @Autowired
    private NotificationMetadataEntityDao<Key, NotificationMetadataEntity> notificationMetadataEntityDao;

    @Test
    void searchNotificationBySenderMetadata() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( true )
                .startDate( Instant.parse( "2022-02-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-04-30T00:00:00.00Z" ) )
                .senderReceiverId( "MI" )
                .size( 10 )
                .nextPagesKey( null )
                .build();
        List<ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey>> resultList = new ArrayList<>();
        PnLastEvaluatedKey lastEvaluatedKey = null;
        do {
            ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result =  notificationMetadataEntityDao.searchNotificationMetadata( inputSearch, lastEvaluatedKey );
            if (!result.getNextPagesKey().isEmpty() ) {
                lastEvaluatedKey = result.getNextPagesKey().get( 0 );
            } else {
                lastEvaluatedKey = null;
            }
            resultList.add( result );
        } while (lastEvaluatedKey !=null);
        /*Map<String, AttributeValue> internalLastEvaluatedKey = new HashMap<>();
        internalLastEvaluatedKey.put( "iun_recipientId", AttributeValue.builder().s( "0020##PF003" ).build() );
        internalLastEvaluatedKey.put( "sentAt", AttributeValue.builder().s( "2022-03-20T20:20:20Z" ).build() );
        internalLastEvaluatedKey.put( "senderId_creationMonth", AttributeValue.builder().s( "MI##202203" ).build() );

        PnLastEvaluatedKey pnLastEvaluatedKey = new PnLastEvaluatedKey();
        pnLastEvaluatedKey.setExternalLastEvaluatedKey( "MI##202203" );
        pnLastEvaluatedKey.setInternalLastEvaluatedKey( internalLastEvaluatedKey );*/
    }

    @Test
    void searchNotificationByRecipientMetadata() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( false )
                .startDate( Instant.parse( "2022-02-16T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-03-18T00:00:00.00Z" ) )
                .senderReceiverId( "PF003" )
                .size( 10 )
                .nextPagesKey( null )
                .build();

        List<ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey>> resultList = new ArrayList<>();
        PnLastEvaluatedKey lastEvaluatedKey = null;
        do {
            ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result =  notificationMetadataEntityDao.searchNotificationMetadata( inputSearch, lastEvaluatedKey );
            if (!result.getNextPagesKey().isEmpty() ) {
                lastEvaluatedKey = result.getNextPagesKey().get( 0 );
            } else {
                lastEvaluatedKey = null;
            }
            resultList.add( result );
        } while (lastEvaluatedKey !=null);
    }

    @Test
    void searchNotificationMetadataWithStatusFilter() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( true )
                .startDate( Instant.parse( "2022-02-16T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-03-18T00:00:00.00Z" ) )
                .senderReceiverId( "MI" )
                .size( 10 )
                .status( NotificationStatus.DELIVERED )
                .build();

        List<ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey>> resultList = new ArrayList<>();
        PnLastEvaluatedKey lastEvaluatedKey = null;
        do {
            ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result =  notificationMetadataEntityDao.searchNotificationMetadata( inputSearch, lastEvaluatedKey );
            if (!result.getNextPagesKey().isEmpty() ) {
                lastEvaluatedKey = result.getNextPagesKey().get( 0 );
            } else {
                lastEvaluatedKey = null;
            }
            resultList.add( result );
        } while (lastEvaluatedKey !=null);
    }

    @Test
    void searchNotificationMetadataWithStatusAndGroupsFilter() {
        //Given
        List<String> groupList = new ArrayList<>();
        groupList.add( "NotificationGroup" );
        groupList.add( "NotificationGroup1" );


        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( true )
                .startDate( Instant.parse( "2022-03-16T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-03-18T00:00:00.00Z" ) )
                .senderReceiverId( "SenderId" )
                .status( NotificationStatus.ACCEPTED )
                .groups( groupList  )
                .build();

        //List<NotificationSearchRow> result = notificationMetadataEntityDao.searchNotificationMetadata( inputSearch );
    }

    @Test
    void putIfAbsent() throws IdConflictException {
        //Given
        NotificationMetadataEntity metadataEntityToInsert = newNotificationMetadata();

        Key key = Key.builder()
                .partitionValue(metadataEntityToInsert.getIun_recipientId())
                .sortValue( metadataEntityToInsert.getSentAt().toString() )
                .build();
        //When
        notificationMetadataEntityDao.putIfAbsent( metadataEntityToInsert );

        //Then
        Optional<NotificationMetadataEntity> elementFromDb = notificationMetadataEntityDao.get( key );

        Assertions.assertTrue( elementFromDb.isPresent() );
        Assertions.assertEquals( metadataEntityToInsert, elementFromDb.get() );
    }

    private NotificationMetadataEntity newNotificationMetadata() {
        Map<String,String> tableRowMap = new HashMap<>();
        tableRowMap.put( "iun", "IUN" );
        tableRowMap.put( "recipientsIds", "[PF003]" );
        tableRowMap.put( "subject", "Notifica IUN" );
        return NotificationMetadataEntity.builder()
                .iun_recipientId( "IUN##RecipientId" )
                .notificationGroup( "NotificationGroup1" )
                .notificationStatus( NotificationStatus.ACCEPTED.toString() )
                .recipientIds( Collections.singletonList("RecipientId") )
                .recipientOne( true )
                .senderId( "SenderId" )
                .recipientId_creationMonth( "RecipientId##202203" )
                .senderId_creationMonth("SenderId##202203")
                .senderId_recipientId( "SenderId##RecipientId" )
                .sentAt( Instant.parse( "2022-03-17T17:51:00.00Z" ) )
                .tableRow( tableRowMap )
                .build();
    }
}