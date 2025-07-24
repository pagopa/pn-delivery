package it.pagopa.pn.delivery.svc.search;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV28;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

class NotificationSearchMultiPageByPFOrPGTest {

    private static final int PAGE_SIZE = 10;

    private NotificationDao notificationDao;
    private InputSearchNotificationDto inputSearchNotificationDto;
    private PnDeliveryConfigs cfg;
    private PnDataVaultClientImpl dataVaultClient;
    private NotificationSearchMultiPageByPFOrPG notificationSearchMultiPageByPFOrPG;
    private EntityToDtoNotificationMetadataMapper entityToDtoNotificationMetadataMapper;

    @BeforeEach
    void setup() {
        this.notificationDao = Mockito.mock(NotificationDao.class);
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );
        this.entityToDtoNotificationMetadataMapper = Mockito.mock(EntityToDtoNotificationMetadataMapper.class);
        this.dataVaultClient = Mockito.mock( PnDataVaultClientImpl.class );
        this.inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.now().minus(500, ChronoUnit.DAYS) )
                .endDate( Instant.now() )
                .size( PAGE_SIZE )
                .build();

        Mockito.when(entityToDtoNotificationMetadataMapper.entity2Dto((NotificationMetadataEntity) Mockito.any()))
                .thenReturn(NotificationSearchRow.builder()
                        .recipients(List.of("recipientId1"))
                        .build());

        BaseRecipientDto baseRecipientDto = new BaseRecipientDto();
        baseRecipientDto.setInternalId("recipientId1");
        baseRecipientDto.setDenomination("nome cognome");
        baseRecipientDto.setTaxId("EEEEEEEEEEEEE");
        Mockito.when(dataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(List.of(baseRecipientDto));


        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(inputSearchNotificationDto);

        this.notificationSearchMultiPageByPFOrPG = new NotificationSearchMultiPageByPFOrPG(notificationDao, entityToDtoNotificationMetadataMapper, inputSearchNotificationDto, null, cfg, dataVaultClient, indexNameAndPartitions);
    }

    @Test
    void mapperException() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(Collections.singletonList( NotificationMetadataEntity.builder()
                .iunRecipientId("IUN##internalId1" )
                .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                .senderId( "SenderId" )
                .sentAt(Instant.now())
                .recipientIds(List.of( "internalId1", "internalId2" ) )
                .build() ));
        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(rrr).thenReturn(new PageSearchTrunk<>());

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        Mockito.when(entityToDtoNotificationMetadataMapper.entity2Dto((NotificationMetadataEntity) Mockito.any()))
                .thenThrow(PnInternalException.class);

        Executable todo = () -> this.notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();
        Assertions.assertThrows( PnInternalException.class, todo);
    }

    @Test
    void searchNotificationMetadata() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(Collections.singletonList( NotificationMetadataEntity.builder()
                .iunRecipientId("IUN##internalId1" )
                .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                .senderId( "SenderId" )
                .sentAt(Instant.now())
                .recipientIds(List.of( "internalId1", "internalId2" ) )
                .build() ));
        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                        .thenReturn(rrr).thenReturn(new PageSearchTrunk<>());

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(1, result.getResultsPage().size());
        Assertions.assertEquals(0, result.getNextPagesKey().size());
        Assertions.assertFalse(result.isMoreResult());
    }


    @Test
    void searchNotificationMetadataMany() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(new ArrayList<>());
        for(int i = 0;i<1000;i++)
        {
            rrr.getResults().add( NotificationMetadataEntity.builder()
                    .iunRecipientId("IUN##internalId"+i )
                    .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                    .senderId( "SenderId" )
                    .sentAt(Instant.now())
                    .recipientIds(List.of( "internalId"+i ) )
                    .build() );
        }


        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(rrr);


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(3, result.getNextPagesKey().size());
        Assertions.assertTrue(result.isMoreResult());
    }


    @Test
    void searchNotificationMetadataManyPageSize() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(new ArrayList<>());
        for(int i = 0;i<PAGE_SIZE;i++)
        {
            rrr.getResults().add( NotificationMetadataEntity.builder()
                    .iunRecipientId("IUN##internalId"+i )
                    .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                    .senderId( "SenderId" )
                    .sentAt(Instant.now())
                    .recipientIds(List.of( "internalId"+i ) )
                    .build() );
        }


        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(rrr).thenReturn(new PageSearchTrunk<>());


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(0, result.getNextPagesKey().size());
        Assertions.assertFalse(result.isMoreResult());
    }

    @Test
    void searchNotificationMetadataManyPageSizeMaxSize() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(new ArrayList<>());
        for(int i = 0;i<PAGE_SIZE*3;i++)
        {
            rrr.getResults().add( NotificationMetadataEntity.builder()
                    .iunRecipientId("IUN##internalId"+i )
                    .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                    .senderId( "SenderId" )
                    .sentAt(Instant.now())
                    .recipientIds(List.of( "internalId"+i ) )
                    .build() );
        }


        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(rrr).thenReturn(new PageSearchTrunk<>());


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(2, result.getNextPagesKey().size());
        Assertions.assertFalse(result.isMoreResult());
    }

    @Test
    void searchNotificationMetadataManyPageSizeMaxSizePlus1() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(new ArrayList<>());
        for(int i = 0;i<PAGE_SIZE*3+1;i++)
        {
            rrr.getResults().add( NotificationMetadataEntity.builder()
                    .iunRecipientId("IUN##internalId"+i )
                    .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                    .senderId( "SenderId" )
                    .sentAt(Instant.now())
                    .recipientIds(List.of( "internalId"+i ) )
                    .build() );
        }


        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(rrr).thenReturn(new PageSearchTrunk<>());


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(3, result.getNextPagesKey().size());
        Assertions.assertTrue(result.isMoreResult());
    }


    @Test
    void searchNotificationMetadataManyButNotAll() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(new ArrayList<>());
        for(int i = 0;i<25;i++)
        {
            rrr.getResults().add( NotificationMetadataEntity.builder()
                    .iunRecipientId("IUN##internalId"+i )
                    .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                    .senderId( "SenderId" )
                    .sentAt(Instant.now())
                    .recipientIds(List.of( "internalId"+i ) )
                    .build() );
        }


        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(rrr).thenReturn(new PageSearchTrunk<>());


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(2, result.getNextPagesKey().size());
        Assertions.assertFalse(result.isMoreResult());
    }

    @Test
    void searchNotificationMetadataManyPaged() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(new ArrayList<>());
        for(int i = 0;i<1;i++)
        {
            rrr.getResults().add( NotificationMetadataEntity.builder()
                    .iunRecipientId("IUN##internalId"+i )
                    .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                    .senderId( "SenderId" )
                    .sentAt(Instant.now())
                    .recipientIds(List.of( "internalId"+i ) )
                    .build() );
        }
        rrr.setLastEvaluatedKey(new HashMap<>());


        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(rrr);


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(3, result.getNextPagesKey().size());
        Assertions.assertTrue(result.isMoreResult());
    }


    @Test
    void searchNotificationMetadataLastKeySet() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(new ArrayList<>());
        for(int i = 0;i<1;i++)
        {
            rrr.getResults().add( NotificationMetadataEntity.builder()
                    .iunRecipientId("IUN##internalId"+i )
                    .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                    .senderId( "SenderId" )
                    .sentAt(Instant.now())
                    .recipientIds(List.of( "internalId"+i ) )
                    .build() );
        }
        rrr.setLastEvaluatedKey(new HashMap<>());
        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(inputSearchNotificationDto);
        PnLastEvaluatedKey lastEvaluatedKey = new PnLastEvaluatedKey();
        lastEvaluatedKey.setExternalLastEvaluatedKey(indexNameAndPartitions.getPartitions().get(1));
        this.notificationSearchMultiPageByPFOrPG = new NotificationSearchMultiPageByPFOrPG(notificationDao, entityToDtoNotificationMetadataMapper, inputSearchNotificationDto, lastEvaluatedKey, cfg, dataVaultClient, indexNameAndPartitions);


        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(rrr);


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(3, result.getNextPagesKey().size());
        Assertions.assertTrue(result.isMoreResult());
    }


    @Test
    void searchNotificationMetadataOtherIndex() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(new ArrayList<>());
        for(int i = 0;i<1;i++)
        {
            rrr.getResults().add( NotificationMetadataEntity.builder()
                    .iunRecipientId("IUN##internalId"+i )
                    .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                    .senderId( "SenderId" )
                    .sentAt(Instant.now())
                    .recipientIds(List.of( "internalId"+i ) )
                    .build() );
        }
        rrr.setLastEvaluatedKey(new HashMap<>());

        this.inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( false )
                .startDate( Instant.now().minus(500, ChronoUnit.DAYS) )
                .endDate( Instant.now() )
                .size( PAGE_SIZE )
                .build();


        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(inputSearchNotificationDto);
        PnLastEvaluatedKey lastEvaluatedKey = new PnLastEvaluatedKey();
        lastEvaluatedKey.setExternalLastEvaluatedKey(indexNameAndPartitions.getPartitions().get(1));
        this.notificationSearchMultiPageByPFOrPG = new NotificationSearchMultiPageByPFOrPG(notificationDao, entityToDtoNotificationMetadataMapper, inputSearchNotificationDto, lastEvaluatedKey, cfg, dataVaultClient, indexNameAndPartitions);


        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(rrr);


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(3, result.getNextPagesKey().size());
        Assertions.assertTrue(result.isMoreResult());
    }


    @Test
    void searchNotificationMetadataOtherIndexBoth() {
        PageSearchTrunk<NotificationMetadataEntity> rrr = new PageSearchTrunk<>();
        rrr.setResults(new ArrayList<>());
        for(int i = 0;i<1;i++)
        {
            rrr.getResults().add( NotificationMetadataEntity.builder()
                    .iunRecipientId("IUN##internalId"+i )
                    .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                    .senderId( "SenderId" )
                    .sentAt(Instant.now())
                    .recipientIds(List.of( "internalId"+i ) )
                    .build() );
        }
        rrr.setLastEvaluatedKey(new HashMap<>());

        this.inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( false )
                .startDate( Instant.now().minus(500, ChronoUnit.DAYS) )
                .endDate( Instant.now() )
                .filterId("internalid123")
                .size( PAGE_SIZE )
                .build();


        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(inputSearchNotificationDto);
        PnLastEvaluatedKey lastEvaluatedKey = new PnLastEvaluatedKey();
        lastEvaluatedKey.setExternalLastEvaluatedKey(indexNameAndPartitions.getPartitions().get(0));
        this.notificationSearchMultiPageByPFOrPG = new NotificationSearchMultiPageByPFOrPG(notificationDao, entityToDtoNotificationMetadataMapper, inputSearchNotificationDto, lastEvaluatedKey, cfg, dataVaultClient, indexNameAndPartitions);


        Mockito.when(notificationDao.searchForOneMonth(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(rrr);


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFOrPG.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(3, result.getNextPagesKey().size());
        Assertions.assertTrue(result.isMoreResult());
    }
}
