package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.LocalStackTestConfig;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV28;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        NotificationMetadataEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "pn.delivery.notification-dao.table-name=Notifications",
        "pn.delivery.notification-metadata-dao.table-name=NotificationsMetadata",
        "pn.delivery.max-recipients-count=0",
        "pn.delivery.max-attachments-count=0"
})
@SpringBootTest
@Import(LocalStackTestConfig.class)
class NotificationMetadataEntityDaoDynamoTestIT {

    private static final String IUN = "KSAU-CKOB-OFKR-202205-O-1";
    private static final String OPAQUE_TAX_ID_R1 = "TFZMREFBODVUNTBHNzAyQg==";
    private static final String TAX_ID_R1 = "LVLDAA85T50G702B";
    private static final String DENOMINATION_R1 = "Ada Lovelace";
    private static final String OPAQUE_TAX_ID_R2 = "Q0xNQ1NUNDJSMTJEOTY5Wg==";
    private static final String TAX_ID_R2 = "CLMCST42R12D969Z";
    private static final String DENOMINATION_R2 = "Cristoforo Colombo";
    private static final String ACCEPTED_DATE = "2022-08-04T13:27Z";

    @Autowired
    private NotificationMetadataEntityDao notificationMetadataEntityDao;

    @MockBean
    private PnDataVaultClientImpl dataVaultClient;



    @Test
    void searchNotificationMetadataBySender() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "c_h501" )
                .size( 10 )
                .nextPagesKey( null )
                .build();
        String indexName = "senderId";
        String partitionValue = "c_h501##202205";

        List<BaseRecipientDto> dataVaultResults = getDataVaultResults();

        Mockito.when( dataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( dataVaultResults );

        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                null
        );

        //Then
        Assertions.assertNotNull( result );

        /*List<ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey>> resultList = new ArrayList<>();
        PnLastEvaluatedKey lastEvaluatedKey = null;
        do {
            ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result =  notificationMetadataEntityDao.searchNotificationMetadata( inputSearch, lastEvaluatedKey );
            if (!result.getNextPagesKey().isEmpty() ) {
                lastEvaluatedKey = result.getNextPagesKey().get( 0 );
            } else {
                lastEvaluatedKey = null;
            }
            resultList.add( result );
        } while (lastEvaluatedKey !=null);*/
        /*Map<String, AttributeValue> internalLastEvaluatedKey = new HashMap<>();
        internalLastEvaluatedKey.put( "iun_recipientId", AttributeValue.builder().s( "0020##PF003" ).build() );
        internalLastEvaluatedKey.put( "sentAt", AttributeValue.builder().s( "2022-03-20T20:20:20Z" ).build() );
        internalLastEvaluatedKey.put( "senderId_creationMonth", AttributeValue.builder().s( "MI##202203" ).build() );

        PnLastEvaluatedKey pnLastEvaluatedKey = new PnLastEvaluatedKey();
        pnLastEvaluatedKey.setExternalLastEvaluatedKey( "MI##202203" );
        pnLastEvaluatedKey.setInternalLastEvaluatedKey( internalLastEvaluatedKey );*/
    }

    @NotNull
    private List<BaseRecipientDto> getDataVaultResults() {
        List<BaseRecipientDto> dataVaultResults = List.of( new BaseRecipientDto()
                .internalId( OPAQUE_TAX_ID_R1 )
                .taxId( TAX_ID_R1 )
                .denomination( DENOMINATION_R1 )
                .recipientType( RecipientType.PF ),
                new BaseRecipientDto()
                        .internalId( OPAQUE_TAX_ID_R2 )
                        .taxId( TAX_ID_R2 )
                        .denomination( DENOMINATION_R2 )
                        .recipientType( RecipientType.PF )
                );
        return dataVaultResults;
    }

    @Test
    void searchNotificationMetadataNextPageBySender() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "c_h501" )
                .size( 10 )
                .nextPagesKey( "eyJlayI6ImNfYjQyOSMjMjAyMjA0IiwiaWsiOnsiaXVuX3JlY2lwaWVudElkIjoiY19iNDI5LTIwMjIwNDA0MTYwNCMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwic2VudEF0IjoiMjAyMi0wNC0wNFQxNDowNDowNy41MjA1NThaIiwic2VuZGVySWRfY3JlYXRpb25Nb250aCI6ImNfYjQyOSMjMjAyMjA0In19" )
                .build();

        String indexName = "senderId";
        String partitionValue = "c_h501##202205";

        PnLastEvaluatedKey lek = new PnLastEvaluatedKey();
        lek.setExternalLastEvaluatedKey( "c_b429##202204" );
        lek.setInternalLastEvaluatedKey( Map.ofEntries(
                        Map.entry( "iun_recipientId", AttributeValue.builder()
                                .s( "c_b429-202204041604##ed84b8c9-444e-410d-80d7-cfad6aa12070" )
                                .build() ),
                        Map.entry( "sentAt", AttributeValue.builder().s("2022-04-04T14:04:07.520558Z")
                                .build() ),
                        Map.entry( "senderId_creationMonth", AttributeValue.builder().s("c_b429##202204")
                                .build() )
                        )
        );

        List<BaseRecipientDto> dataVaultResults = getDataVaultResults();

        Mockito.when( dataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( dataVaultResults );

        //When
        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                lek
        );

        //Then
        Assertions.assertNotNull( result );
    }

    @Test
    void searchNotificationMetadataByRecipient() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( false )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( TAX_ID_R1 )
                .size( 10 )
                .nextPagesKey( null )
                .build();

        String indexName = "recipientId";
        String partitionValue = OPAQUE_TAX_ID_R1+"##202205";

        List<BaseRecipientDto> dataVaultResults = getDataVaultResults();

        Mockito.when( dataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( dataVaultResults );

        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                null
        );

        Assertions.assertNotNull( result );
    }

    @Test
    void searchNotificationMetadataNextPageByRecipient() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( false )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( OPAQUE_TAX_ID_R1 )
                .size( 10 )
                .nextPagesKey( "eyJlayI6ImVkODRiOGM5LTQ0NGUtNDEwZC04MGQ3LWNmYWQ2YWExMjA3MCMjMjAyMjA0IiwiaWsiOnsiaXVuX3JlY2lwaWVudElkIjoiY19iNDI5LTIwMjIwNDA0MTYwNCMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwicmVjaXBpZW50SWRfY3JlYXRpb25Nb250aCI6ImVkODRiOGM5LTQ0NGUtNDEwZC04MGQ3LWNmYWQ2YWExMjA3MCMjMjAyMjA0Iiwic2VudEF0IjoiMjAyMi0wNC0wNFQxNDowNDowNy41MjA1NThaIn19" )
                .build();

        String indexName = "recipientId";
        String partitionValue = OPAQUE_TAX_ID_R1+"##202205";

        PnLastEvaluatedKey lek = new PnLastEvaluatedKey();
        lek.setExternalLastEvaluatedKey( "ed84b8c9-444e-410d-80d7-cfad6aa12070##202204" );
        lek.setInternalLastEvaluatedKey( Map.ofEntries(
                        Map.entry( "iun_recipientId", AttributeValue.builder()
                                .s( "c_b429-202204041604##ed84b8c9-444e-410d-80d7-cfad6aa12070" )
                                .build() ),
                        Map.entry( "sentAt", AttributeValue.builder().s("2022-04-04T14:04:07.520558Z")
                                .build() ),
                        Map.entry( "recipientId_creationMonth", AttributeValue.builder().s("ed84b8c9-444e-410d-80d7-cfad6aa12070##202204")
                                .build() )
                )
        );

        List<BaseRecipientDto> dataVaultResults = getDataVaultResults();

        Mockito.when( dataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( dataVaultResults );

        //When
        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                lek
        );

        //Then
        Assertions.assertNotNull( result );

    }

    @Test
    void searchNotificationMetadataWithRecipientFilter() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "c_h501" )
                .size( 10 )
                .filterId( OPAQUE_TAX_ID_R1 )
                .build();

        String indexName = "senderId_recipientId";
        String partitionValue = "c_h501##"+OPAQUE_TAX_ID_R1;

        List<BaseRecipientDto> dataVaultResults = getDataVaultResults();

        Mockito.when( dataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( dataVaultResults );

        //When
        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                null
        );

        //Then
        Assertions.assertNotNull( result );
    }

    @Test
    void searchNotificationMetadataWithNextPageRecipientFilter() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "c_h501" )
                .size( 10 )
                .nextPagesKey( "eyJlayI6ImNfYjQyOSMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwiaWsiOnsiaXVuX3JlY2lwaWVudElkIjoiY19iNDI5LTIwMjIwNDA1MTEyOCMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwic2VudEF0IjoiMjAyMi0wNC0wNVQwOToyODo0Mi4zNTgxMzZaIiwic2VuZGVySWRfcmVjaXBpZW50SWQiOiJjX2I0MjkjI2VkODRiOGM5LTQ0NGUtNDEwZC04MGQ3LWNmYWQ2YWExMjA3MCJ9fQ==" )
                .filterId( OPAQUE_TAX_ID_R1 )
                .build();

        String indexName = "senderId_recipientId";
        String partitionValue = "c_h501##"+OPAQUE_TAX_ID_R1;

        PnLastEvaluatedKey lek = new PnLastEvaluatedKey();
        lek.setExternalLastEvaluatedKey( "ed84b8c9-444e-410d-80d7-cfad6aa12070##202204" );
        lek.setInternalLastEvaluatedKey( Map.ofEntries(
                        Map.entry( "iun_recipientId", AttributeValue.builder()
                                .s( "c_b429-202204041604##ed84b8c9-444e-410d-80d7-cfad6aa12070" )
                                .build() ),
                        Map.entry( "sentAt", AttributeValue.builder().s("2022-04-04T14:04:07.520558Z")
                                .build() ),
                        Map.entry( "senderId_recipientId", AttributeValue.builder().s("c_b429##ed84b8c9-444e-410d-80d7-cfad6aa12070")
                                .build() )
                )
        );

        List<BaseRecipientDto> dataVaultResults = getDataVaultResults();

        Mockito.when( dataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( dataVaultResults );

        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                lek
        );

        Assertions.assertNotNull( result );
    }

    @Test
    void searchNotificationMetadataWithStatusFilter() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "c_h501" )
                .size( 10 )
                .statuses(List.of(NotificationStatusV28.ACCEPTED))
                .build();

        String indexName = "senderId";
        String partitionValue = "c_h501##202205";

        List<BaseRecipientDto> dataVaultResults = getDataVaultResults();

        Mockito.when( dataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( dataVaultResults );

        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                null
        );

        Assertions.assertNotNull( result );

    }

    @Test
    void searchNotificationMetadataWithGroupsFilter_grp_and_nogrp() {
        //Given
        String recipientId = "recipientId";
        String senderPaid1 = "paid1";
        String senderPaid2 = "paid2";


        List<String> groups = new ArrayList<>();
        groups.add("Group1");

        List<String> paids = new ArrayList<>();
        paids.add(senderPaid1);
        paids.add(senderPaid2);

        Instant sentAt = Instant.parse( "2022-05-28T00:00:00.00Z" );

        InternalNotification internalNotification1 = new InternalNotification();
        internalNotification1.setIun("IUN-1");
        internalNotification1.setGroup(groups.get(0));
        internalNotification1.setSenderPaId(senderPaid1);
        internalNotification1.setSentAt(sentAt.atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity1 = buildOneSearchMetadataEntry(
                internalNotification1,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt.atOffset(ZoneOffset.UTC)
        );

        InternalNotification internalNotification2 = new InternalNotification();
        internalNotification2.setIun("IUN-2");
        internalNotification2.setSenderPaId(senderPaid1);
        internalNotification2.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        Instant sentAt1 = Instant.parse( "2022-05-29T00:00:00.00Z" );

        NotificationMetadataEntity notificationMetadataEntity2 = buildOneSearchMetadataEntry(
                internalNotification2,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt1.atOffset(ZoneOffset.UTC)
        );

        notificationMetadataEntityDao.put(notificationMetadataEntity1);
        notificationMetadataEntityDao.put(notificationMetadataEntity2);


        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( senderPaid1 )
                .size( 10 )
                .iunMatch( IUN )
                .build();

        String indexName = "senderId";
        String partitionValue = createConcatenation(senderPaid1, "202205");

        List<BaseRecipientDto> dataVaultResults = getDataVaultResults();

        Mockito.when( dataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( dataVaultResults );

        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                null
        );


        Assertions.assertNotNull( result );
        Assertions.assertEquals( createConcatenation(internalNotification1.getIun(), recipientId), result.getResults().get(0).getIunRecipientId() );
        Assertions.assertEquals( createConcatenation(internalNotification2.getIun(), recipientId), result.getResults().get(1).getIunRecipientId() );
    }

    @Test
    void searchNotificationMetadataWithGroupsFilter() {
        //Given
        String recipientId = "recipientId";
        String senderPaid1 = "paid1";
        String senderPaid2 = "paid2";

        List<String> groups = new ArrayList<>();
        groups.add("Group1");


        List<String> paids = new ArrayList<>();
        paids.add(senderPaid1);
        paids.add(senderPaid2);

        Instant sentAt = Instant.parse( "2022-05-28T00:00:00.00Z" );

        InternalNotification internalNotification1 = new InternalNotification();
        internalNotification1.setIun("IUN-1");
        internalNotification1.setGroup(groups.get(0));
        internalNotification1.setSenderPaId(senderPaid1);
        internalNotification1.setSentAt(sentAt.atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity1 = buildOneSearchMetadataEntry(
                internalNotification1,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt.atOffset(ZoneOffset.UTC)
        );

        InternalNotification internalNotification2 = new InternalNotification();
        internalNotification2.setIun("IUN-2");
        internalNotification2.setSenderPaId(senderPaid2);
        internalNotification2.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        Instant sentAt1 = Instant.parse( "2022-05-29T00:00:00.00Z" );

        NotificationMetadataEntity notificationMetadataEntity2 = buildOneSearchMetadataEntry(
                internalNotification2,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt1.atOffset(ZoneOffset.UTC)
        );

        notificationMetadataEntityDao.put(notificationMetadataEntity1);
        notificationMetadataEntityDao.put(notificationMetadataEntity2);


        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( senderPaid1 )
                .size( 10 )
                .groups( groups )
                .build();

        String indexName = "senderId";
        String partitionValue = createConcatenation(senderPaid1, "202205");

        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                null
        );

        Assertions.assertNotNull( result );
        Assertions.assertEquals( createConcatenation(internalNotification1.getIun(), recipientId), result.getResults().get(0).getIunRecipientId() );
    }


    @Test
    void searchNotificationMetadataWithGroupsFilter_2groups() {
        //Given
        String recipientId = "recipientId";
        String senderPaid1 = "paid1";
        String senderPaid2 = "paid2";

        List<String> groups = new ArrayList<>();
        groups.add("Group1");
        groups.add("Group2");


        List<String> paids = new ArrayList<>();
        paids.add(senderPaid1);
        paids.add(senderPaid2);

        Instant sentAt = Instant.parse( "2022-05-28T00:00:00.00Z" );

        InternalNotification internalNotification1 = new InternalNotification();
        internalNotification1.setIun("IUN-1");
        internalNotification1.setGroup(groups.get(0));
        internalNotification1.setSenderPaId(senderPaid1);
        internalNotification1.setSentAt(sentAt.atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity1 = buildOneSearchMetadataEntry(
                internalNotification1,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt.atOffset(ZoneOffset.UTC)
        );

        InternalNotification internalNotification2 = new InternalNotification();
        internalNotification2.setIun("IUN-2");
        internalNotification2.setSenderPaId(senderPaid2);
        internalNotification2.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        Instant sentAt1 = Instant.parse( "2022-05-29T00:00:00.00Z" );

        NotificationMetadataEntity notificationMetadataEntity2 = buildOneSearchMetadataEntry(
                internalNotification2,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt1.atOffset(ZoneOffset.UTC)
        );

        notificationMetadataEntityDao.put(notificationMetadataEntity1);
        notificationMetadataEntityDao.put(notificationMetadataEntity2);



        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( senderPaid1)
                .size( 10 )
                .groups( groups )
                .build();

        String indexName = "senderId";
        String partitionValue = createConcatenation(senderPaid1, "202205");

        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                null
        );

        Assertions.assertNotNull( result );
        Assertions.assertEquals( createConcatenation(internalNotification1.getIun(), recipientId), result.getResults().get(0).getIunRecipientId() );
    }


    @Test
    void searchNotificationMetadataWithPAIDsFilter_empty() {
        //Given
        List<String> paids = new ArrayList<>();
        paids.add("paid1");


        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "c_h501" )
                .size( 10 )
                .mandateAllowedPaIds( paids )
                .build();

        String indexName = "senderId";
        String partitionValue = "c_h501##202205";

        PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                inputSearch,
                indexName,
                partitionValue,
                inputSearch.getSize(),
                null
        );

        Assertions.assertNotNull( result );
    }




    @Test
    void searchNotificationMetadataWithPAIDsFilterWithFilter() {
        //Given
        String recipientId = "recipientId";
        String senderPaid1 = "paid1";
        String senderPaid2 = "paid2";


        List<String> paids = new ArrayList<>();
        paids.add(senderPaid1);

        InternalNotification internalNotification1 = new InternalNotification();
        internalNotification1.setIun("IUN-1");
        internalNotification1.setGroup("group");
        internalNotification1.setSenderPaId(senderPaid1);
        internalNotification1.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity1 = buildOneSearchMetadataEntry(
                internalNotification1,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                Instant.parse( "2022-05-29T00:00:00.00Z" ).atOffset(ZoneOffset.UTC)
        );

        InternalNotification internalNotification2 = new InternalNotification();
        internalNotification2.setIun("IUN-2");
        internalNotification2.setGroup("group");
        internalNotification2.setSenderPaId(senderPaid2);
        internalNotification2.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity2 = buildOneSearchMetadataEntry(
                internalNotification2,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                Instant.parse( "2022-05-29T00:00:00.00Z" ).atOffset(ZoneOffset.UTC)
        );

        try {
            notificationMetadataEntityDao.put(notificationMetadataEntity1);
            notificationMetadataEntityDao.put(notificationMetadataEntity2);

            InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                    .bySender( false )
                    .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                    .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                    .senderReceiverId( recipientId )
                    .size( 10 )
                    .mandateAllowedPaIds( paids )
                    .build();

            String indexName = "recipientId";
            String partitionValue = createConcatenation(recipientId , "202205");

            PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                    inputSearch,
                    indexName,
                    partitionValue,
                    inputSearch.getSize(),
                    null
            );

            Assertions.assertNotNull( result );
            Assertions.assertEquals(1, result.getResults().size());
            Assertions.assertEquals(senderPaid1, result.getResults().get(0).getSenderId());
        } catch (Exception e) {
            Assertions.fail(e);
        } finally {
            notificationMetadataEntityDao.delete(Key.builder()
                    .partitionValue(notificationMetadataEntity1.getIunRecipientId())
                    .sortValue(notificationMetadataEntity1.getSentAt().toString())
                    .build());
            notificationMetadataEntityDao.delete(Key.builder()
                    .partitionValue(notificationMetadataEntity2.getIunRecipientId())
                    .sortValue(notificationMetadataEntity2.getSentAt().toString())
                    .build());
        }


    }



    @Test
    void searchNotificationMetadataWithPAIDsFilterWithFilter_both() {
        //Given
        String recipientId = "recipientId";
        String senderPaid1 = "paid1";
        String senderPaid2 = "paid2";


        List<String> paids = new ArrayList<>();
        paids.add(senderPaid1);
        paids.add(senderPaid2);

        InternalNotification internalNotification1 = new InternalNotification();
        internalNotification1.setIun("IUN-1");
        internalNotification1.setGroup("group");
        internalNotification1.setSenderPaId(senderPaid1);
        internalNotification1.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity1 = buildOneSearchMetadataEntry(
                internalNotification1,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                Instant.parse( "2022-05-29T00:00:00.00Z" ).atOffset(ZoneOffset.UTC)
        );

        InternalNotification internalNotification2 = new InternalNotification();
        internalNotification2.setIun("IUN-2");
        internalNotification2.setGroup("group");
        internalNotification2.setSenderPaId(senderPaid2);
        internalNotification2.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity2 = buildOneSearchMetadataEntry(
                internalNotification2,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                Instant.parse( "2022-05-29T00:00:00.00Z" ).atOffset(ZoneOffset.UTC)
        );

        try {
            notificationMetadataEntityDao.put(notificationMetadataEntity1);
            notificationMetadataEntityDao.put(notificationMetadataEntity2);

            InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                    .bySender( false )
                    .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                    .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                    .senderReceiverId( recipientId )
                    .size( 10 )
                    .mandateAllowedPaIds( paids )
                    .build();

            String indexName = "recipientId";
            String partitionValue = createConcatenation(recipientId , "202205");

            PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchForOneMonth(
                    inputSearch,
                    indexName,
                    partitionValue,
                    inputSearch.getSize(),
                    null
            );

            Assertions.assertNotNull( result );
            Assertions.assertEquals(2, result.getResults().size());

        } catch (Exception e) {
            Assertions.fail(e);
        } finally {
            notificationMetadataEntityDao.delete(Key.builder()
                    .partitionValue(notificationMetadataEntity1.getIunRecipientId())
                    .sortValue(notificationMetadataEntity1.getSentAt().toString())
                    .build());
            notificationMetadataEntityDao.delete(Key.builder()
                    .partitionValue(notificationMetadataEntity2.getIunRecipientId())
                    .sortValue(notificationMetadataEntity2.getSentAt().toString())
                    .build());
        }


    }




    @Test
    void searchByIun_notfound() {
        //Given
        String recipientId = "recipientId";
        String senderPaid1 = "paid1";
        String senderPaid2 = "paid2";


        List<String> paids = new ArrayList<>();
        paids.add(senderPaid1);
        paids.add(senderPaid2);

        InternalNotification internalNotification1 = new InternalNotification();
        internalNotification1.setIun("IUN-1");
        internalNotification1.setGroup("group");
        internalNotification1.setSenderPaId(senderPaid1);
        internalNotification1.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity1 = buildOneSearchMetadataEntry(
                internalNotification1,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                Instant.parse( "2022-05-29T00:00:00.00Z" ).atOffset(ZoneOffset.UTC)
        );

        InternalNotification internalNotification2 = new InternalNotification();
        internalNotification2.setIun("IUN-2");
        internalNotification2.setGroup("group");
        internalNotification2.setSenderPaId(senderPaid2);
        internalNotification2.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity2 = buildOneSearchMetadataEntry(
                internalNotification2,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                Instant.parse( "2022-05-29T00:00:00.00Z" ).atOffset(ZoneOffset.UTC)
        );

        try {

            InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                    .bySender( false )
                    .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                    .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                    .senderReceiverId( recipientId )
                    .size( 10 )
                    .mandateAllowedPaIds( paids )
                    .build();

            String indexName = "recipientId";
            String partitionValue = createConcatenation(recipientId , "202205");

            PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchByIun(
                    inputSearch,
                    partitionValue,"test"
            );

            Assertions.assertNotNull( result );
            Assertions.assertNull(result.getResults());

        } catch (Exception e) {
            Assertions.fail(e);
        } finally {

        }


    }



    @Test
    void searchByIun_nullgroup() {
        //Givend
        String recipientId = "recipientId";
        String senderPaid1 = "paid1";
        String senderPaid2 = "paid2";


        List<String> paids = new ArrayList<>();
        paids.add(senderPaid1);
        paids.add(senderPaid2);

        Instant sentAt = Instant.parse( "2022-05-28T00:00:00.00Z" );

        InternalNotification internalNotification1 = new InternalNotification();
        internalNotification1.setIun("IUN-1"); 
        internalNotification1.setSenderPaId(senderPaid1);
        internalNotification1.setSentAt(sentAt.atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity1 = buildOneSearchMetadataEntry(
                internalNotification1,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt.atOffset(ZoneOffset.UTC)
        );

        InternalNotification internalNotification2 = new InternalNotification();
        internalNotification2.setIun("IUN-2");
        internalNotification2.setSenderPaId(senderPaid2);
        internalNotification2.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        Instant sentAt1 = Instant.parse( "2022-05-29T00:00:00.00Z" );

        NotificationMetadataEntity notificationMetadataEntity2 = buildOneSearchMetadataEntry(
                internalNotification2,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt1.atOffset(ZoneOffset.UTC)
        );

        try {

            notificationMetadataEntityDao.put(notificationMetadataEntity1);
            notificationMetadataEntityDao.put(notificationMetadataEntity2);

            InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                    .bySender( true )
                    .iunMatch(internalNotification1.getIun())
                    .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                    .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                    .senderReceiverId( senderPaid1 )
                    .size( 10 )
                    .mandateAllowedPaIds( paids )
                    .build();

            String indexName = "recipientId";
            String partitionValue = createConcatenation(internalNotification1.getIun() , recipientId);

            PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchByIun(
                    inputSearch,
                    partitionValue,sentAt.toString()
            );

            Assertions.assertNotNull( result );
            Assertions.assertEquals(1, result.getResults().size());

        } catch (Exception e) {
            Assertions.fail(e);
        } finally {

        }


    }



    @Test
    void searchByIun_somegroup() {
        //Givend
        String recipientId = "recipientId";
        String senderPaid1 = "paid1";
        String senderPaid2 = "paid2";


        List<String> paids = new ArrayList<>();
        paids.add(senderPaid1);
        paids.add(senderPaid2);

        Instant sentAt = Instant.parse( "2022-05-28T00:00:00.00Z" );

        InternalNotification internalNotification1 = new InternalNotification();
        internalNotification1.setIun("IUN-1");
        internalNotification1.setGroup("gruppo1");
        internalNotification1.setSenderPaId(senderPaid1);
        internalNotification1.setSentAt(sentAt.atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity1 = buildOneSearchMetadataEntry(
                internalNotification1,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt.atOffset(ZoneOffset.UTC)
        );

        InternalNotification internalNotification2 = new InternalNotification();
        internalNotification2.setIun("IUN-2");
        internalNotification2.setSenderPaId(senderPaid2);
        internalNotification2.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        Instant sentAt1 = Instant.parse( "2022-05-29T00:00:00.00Z" );

        NotificationMetadataEntity notificationMetadataEntity2 = buildOneSearchMetadataEntry(
                internalNotification2,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt1.atOffset(ZoneOffset.UTC)
        );

        try {

            notificationMetadataEntityDao.put(notificationMetadataEntity1);
            notificationMetadataEntityDao.put(notificationMetadataEntity2);

            InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                    .bySender( true )
                    .iunMatch(internalNotification1.getIun())
                    .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                    .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                    .senderReceiverId( senderPaid1 )
                    .groups(List.of("gruppo1"))
                    .size( 10 )
                    .mandateAllowedPaIds( paids )
                    .build();

            String indexName = "recipientId";
            String partitionValue = createConcatenation(internalNotification1.getIun() , recipientId);

            PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchByIun(
                    inputSearch,
                    partitionValue,sentAt.toString()
            );

            Assertions.assertNotNull( result );
            Assertions.assertEquals(1, result.getResults().size());

        } catch (Exception e) {
            Assertions.fail(e);
        } finally {

        }


    }



    @Test
    void searchByIun_somegroup_nomatch() {
        //Givend
        String recipientId = "recipientId";
        String senderPaid1 = "paid1";
        String senderPaid2 = "paid2";


        List<String> paids = new ArrayList<>();
        paids.add(senderPaid1);
        paids.add(senderPaid2);

        Instant sentAt = Instant.parse( "2022-05-28T00:00:00.00Z" );

        InternalNotification internalNotification1 = new InternalNotification();
        internalNotification1.setIun("IUN-1");
        internalNotification1.setGroup("gruppo1");
        internalNotification1.setSenderPaId(senderPaid1);
        internalNotification1.setSentAt(sentAt.atOffset(ZoneOffset.UTC));

        NotificationMetadataEntity notificationMetadataEntity1 = buildOneSearchMetadataEntry(
                internalNotification1,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt.atOffset(ZoneOffset.UTC)
        );

        InternalNotification internalNotification2 = new InternalNotification();
        internalNotification2.setIun("IUN-2");
        internalNotification2.setSenderPaId(senderPaid2);
        internalNotification2.setSentAt(Instant.parse( "2022-05-28T00:00:00.00Z" ).atOffset(ZoneOffset.UTC));

        Instant sentAt1 = Instant.parse( "2022-05-29T00:00:00.00Z" );

        NotificationMetadataEntity notificationMetadataEntity2 = buildOneSearchMetadataEntry(
                internalNotification2,
                NotificationStatusV28.ACCEPTED,
                recipientId,
                List.of(recipientId),
                "202205",
                sentAt1.atOffset(ZoneOffset.UTC)
        );

        try {

            notificationMetadataEntityDao.put(notificationMetadataEntity1);
            notificationMetadataEntityDao.put(notificationMetadataEntity2);

            InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                    .bySender( true )
                    .iunMatch(internalNotification1.getIun())
                    .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                    .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                    .senderReceiverId( senderPaid1 )
                    .groups(List.of("gruppo2"))
                    .size( 10 )
                    .mandateAllowedPaIds( paids )
                    .build();

            String indexName = "recipientId";
            String partitionValue = createConcatenation(internalNotification1.getIun() , recipientId);

            PageSearchTrunk<NotificationMetadataEntity> result = notificationMetadataEntityDao.searchByIun(
                    inputSearch,
                    partitionValue,sentAt.toString()
            );

            Assertions.assertNotNull( result );
            Assertions.assertNull(result.getResults());

        } catch (Exception e) {
            Assertions.fail(e);
        } finally {

        }


    }


    @NotNull
    private Map<String, String> createTableRowMap(InternalNotification notification, NotificationStatusV28 lastStatus, List<String> recipientsIds, OffsetDateTime acceptedAt) {
        Map<String,String> tableRowMap = new HashMap<>();
        tableRowMap.put( "iun", notification.getIun() );
        tableRowMap.put( "recipientsIds", recipientsIds.toString() );
        tableRowMap.put( "paProtocolNumber", notification.getPaProtocolNumber() );
        tableRowMap.put( "subject", notification.getSubject() );
        tableRowMap.put( "senderDenomination", notification.getSenderDenomination() );
        if ( Objects.nonNull( acceptedAt )) {
            tableRowMap.put( "acceptedAt", acceptedAt.toString() );
        }
        return tableRowMap;
    }

    private NotificationMetadataEntity buildOneSearchMetadataEntry(
            InternalNotification notification,
            NotificationStatusV28 lastStatus,
            String recipientId,
            List<String> recipientsIds,
            String creationMonth,
            OffsetDateTime acceptedAt
    ) {
        int recipientIndex = recipientsIds.indexOf( recipientId );

        Map<String,String> tableRowMap = createTableRowMap(notification, lastStatus, recipientsIds, acceptedAt);

        return NotificationMetadataEntity.builder()
                .notificationStatus( lastStatus.toString() )
                .senderId( notification.getSenderPaId() )
                .recipientId( recipientId )
                .sentAt( notification.getSentAt().toInstant() )
                .notificationGroup( notification.getGroup() )
                .recipientIds( recipientsIds )
                .tableRow( tableRowMap )
                .senderIdRecipientId( createConcatenation( notification.getSenderPaId(), recipientId  ) )
                .senderIdCreationMonth( createConcatenation( notification.getSenderPaId(), creationMonth ) )
                .recipientIdCreationMonth( createConcatenation( recipientId , creationMonth ) )
                .iunRecipientId( createConcatenation( notification.getIun(), recipientId ) )
                .recipientOne( recipientIndex <= 0 )
                .build();
    }


    private String createConcatenation(String s1, String s2) {
        return s1 + "##" + s2;
    }


    @Test
    void putIfAbsent() throws PnIdConflictException {
        //Given
        NotificationMetadataEntity metadataEntityToInsert = newNotificationMetadata();

        Key key = Key.builder()
                .partitionValue(metadataEntityToInsert.getIunRecipientId())
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
        tableRowMap.put( "iun", IUN );
        tableRowMap.put( "recipientsIds", "["+OPAQUE_TAX_ID_R1+","+ OPAQUE_TAX_ID_R2+"]" );
        tableRowMap.put( "subject", "multa" );
        tableRowMap.put( "acceptedAt", ACCEPTED_DATE );
        return NotificationMetadataEntity.builder()
                .iunRecipientId( "KSAU-CKOB-OFKR-202205-O-1##"+OPAQUE_TAX_ID_R1 )
                .notificationGroup( "NotificationGroup1" )
                .notificationStatus( NotificationStatusV28.ACCEPTED.toString() )
                .recipientIds( List.of( OPAQUE_TAX_ID_R1, OPAQUE_TAX_ID_R2 ) )
                .recipientOne( false )
                .senderId( "c_h501" )
                .recipientIdCreationMonth( OPAQUE_TAX_ID_R1+"##202205" )
                .senderIdCreationMonth("c_h501##202205")
                .senderIdRecipientId( "c_h501##"+OPAQUE_TAX_ID_R1 )
                .sentAt( Instant.parse( "2022-05-20T09:51:00.00Z" ) )
                .tableRow( tableRowMap )
                .build();
    }
}