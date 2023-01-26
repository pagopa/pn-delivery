package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
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
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

class NotificationSearchMultiPageByPFAndPGOnlyTest {

    private static final int PAGE_SIZE = 10;

    private NotificationDao notificationDao;
    private InputSearchNotificationDto inputSearchNotificationDto;
    private PnDeliveryConfigs cfg;
    private PnDataVaultClientImpl dataVaultClient;
    private NotificationSearchMultiPageByPFAndPGOnly notificationSearchMultiPageByPFAndPGOnly;
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
                .filterId( "EEEEEEEEEEEEE" )
                .opaqueFilterIdPG( "opaqueRecipientIdPG" )
                .opaqueFilterIdPF( "opaqueRecipientIdPF" )
                .build();

        Mockito.when(entityToDtoNotificationMetadataMapper.entity2Dto(Mockito.any())).thenReturn(NotificationSearchRow.builder()
                .recipients(List.of("recipientId1"))
                .build());

        BaseRecipientDto baseRecipientDto = new BaseRecipientDto();
        baseRecipientDto.setInternalId("recipientId1");
        baseRecipientDto.setDenomination("nome cognome");
        baseRecipientDto.setTaxId("EEEEEEEEEEEEE");
        Mockito.when(dataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(List.of(baseRecipientDto));


        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(inputSearchNotificationDto);

        this.notificationSearchMultiPageByPFAndPGOnly = new NotificationSearchMultiPageByPFAndPGOnly(notificationDao, entityToDtoNotificationMetadataMapper, inputSearchNotificationDto, null, cfg, dataVaultClient, indexNameAndPartitions);
    }

    @Test
    void searchNotificationMetadataIndexSenderIdRecipientId() {
        PageSearchTrunk<NotificationMetadataEntity> entityResultPF = new PageSearchTrunk<>();
        entityResultPF.setResults(new ArrayList<>());
        PageSearchTrunk<NotificationMetadataEntity> entityResultPG = new PageSearchTrunk<>();
        entityResultPG.setResults(new ArrayList<>());
        Instant now = Instant.now();
        for(int i = 0;i<5;i++)
        {
            entityResultPF.getResults().add( NotificationMetadataEntity.builder()
                    .senderIdRecipientId( "senderId##internalIdPF"+i )
                    .iunRecipientId("IUN##internalIdPF"+i )
                    .notificationStatus( NotificationStatus.VIEWED.getValue() )
                    .senderId( "senderId" )
                    .sentAt(now.plus(Duration.ofDays( i )))
                    .recipientIds(List.of( "internalIdPF"+i ) )
                    .build() );
        }

        for(int i = 0;i<5;i++)
        {
            entityResultPG.getResults().add( NotificationMetadataEntity.builder()
                    .senderIdRecipientId( "senderId##internalIdPG"+i )
                    .iunRecipientId("IUN##internalIdPG"+i )
                    .notificationStatus( NotificationStatus.VIEWED.getValue() )
                    .senderId( "senderId" )
                    .sentAt(now.plus( Duration.ofDays( i ) ).plusSeconds( i ))
                    .recipientIds(List.of( "internalIdPG"+i ) )
                    .build() );
        }
        entityResultPF.setLastEvaluatedKey(null);
        entityResultPG.setLastEvaluatedKey(null);

        this.inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .senderReceiverId( "senderId" )
                .startDate( Instant.now().minus(500, ChronoUnit.DAYS) )
                .endDate( Instant.now() )
                .filterId("externalId")
                .opaqueFilterIdPF( "internalIdPF123" )
                .opaqueFilterIdPG( "internalIdPG123" )
                .size( PAGE_SIZE )
                .build();


        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(inputSearchNotificationDto);
        this.notificationSearchMultiPageByPFAndPGOnly = new NotificationSearchMultiPageByPFAndPGOnly(notificationDao, entityToDtoNotificationMetadataMapper, inputSearchNotificationDto, null, cfg, dataVaultClient, indexNameAndPartitions);


        Mockito.when(notificationDao.searchForOneMonth(inputSearchNotificationDto, indexNameAndPartitions.getIndexName().getValue(), indexNameAndPartitions.getPartitions().get(0), 124, null))
                .thenReturn(entityResultPF);
        Mockito.when(notificationDao.searchForOneMonth(inputSearchNotificationDto, indexNameAndPartitions.getIndexName().getValue(), indexNameAndPartitions.getPartitions().get(1), 124, null))
                .thenReturn(entityResultPG);


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFAndPGOnly.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(0, result.getNextPagesKey().size());
        Assertions.assertFalse(result.isMoreResult());
    }

    @Test
    void searchNotificationMetadataPGthenPF() {
        PageSearchTrunk<NotificationMetadataEntity> entityResultPF = new PageSearchTrunk<>();
        entityResultPF.setResults(new ArrayList<>());
        PageSearchTrunk<NotificationMetadataEntity> entityResultPG = new PageSearchTrunk<>();
        entityResultPG.setResults(new ArrayList<>());
        Instant now = Instant.now();

        for(int i = 0;i<5;i++)
        {
            entityResultPG.getResults().add( NotificationMetadataEntity.builder()
                    .senderIdRecipientId( "senderId##internalIdPG"+i )
                    .iunRecipientId("IUN##internalIdPG"+i )
                    .notificationStatus( NotificationStatus.VIEWED.getValue() )
                    .senderId( "senderId" )
                    .sentAt(now.plus( Duration.ofDays( i ) ))
                    .recipientIds(List.of( "internalIdPG"+i ) )
                    .build() );
        }
        for(int i = 0;i<5;i++)
        {
            entityResultPF.getResults().add( NotificationMetadataEntity.builder()
                    .senderIdRecipientId( "senderId##internalIdPF"+i )
                    .iunRecipientId("IUN##internalIdPF"+i )
                    .notificationStatus( NotificationStatus.VIEWED.getValue() )
                    .senderId( "senderId" )
                    .sentAt(now.plus( Duration.ofDays( i ) ).plusSeconds( i ))
                    .recipientIds(List.of( "internalIdPF"+i ) )
                    .build() );
        }


        entityResultPG.setLastEvaluatedKey(null);
        entityResultPF.setLastEvaluatedKey(null);

        this.inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .senderReceiverId( "senderId" )
                .startDate( Instant.now().minus(500, ChronoUnit.DAYS) )
                .endDate( Instant.now() )
                .filterId("externalId")
                .opaqueFilterIdPF( "internalIdPF123" )
                .opaqueFilterIdPG( "internalIdPG123" )
                .size( PAGE_SIZE )
                .build();


        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(inputSearchNotificationDto);
        this.notificationSearchMultiPageByPFAndPGOnly = new NotificationSearchMultiPageByPFAndPGOnly(notificationDao, entityToDtoNotificationMetadataMapper, inputSearchNotificationDto, null, cfg, dataVaultClient, indexNameAndPartitions);


        Mockito.when(notificationDao.searchForOneMonth(inputSearchNotificationDto, indexNameAndPartitions.getIndexName().getValue(), indexNameAndPartitions.getPartitions().get(0), 124, null))
                .thenReturn(entityResultPF);
        Mockito.when(notificationDao.searchForOneMonth(inputSearchNotificationDto, indexNameAndPartitions.getIndexName().getValue(), indexNameAndPartitions.getPartitions().get(1), 124, null))
                .thenReturn(entityResultPG);


        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFAndPGOnly.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(0, result.getNextPagesKey().size());
        Assertions.assertFalse(result.isMoreResult());
    }

    @Test
    void searchNotificationMetadataIndexSenderIdRecipientIdMoreResult() {
        PageSearchTrunk<NotificationMetadataEntity> entityResultPF = new PageSearchTrunk<>();
        entityResultPF.setResults(new ArrayList<>());
        PageSearchTrunk<NotificationMetadataEntity> entityResultPG = new PageSearchTrunk<>();
        entityResultPG.setResults(new ArrayList<>());
        Instant now = Instant.now();
        for(int i = 0;i<15;i++)
        {
            entityResultPF.getResults().add( NotificationMetadataEntity.builder()
                    .senderIdRecipientId( "senderId##internalIdPF"+i )
                    .iunRecipientId("IUN##internalIdPF"+i )
                    .notificationStatus( NotificationStatus.VIEWED.getValue() )
                    .senderId( "senderId" )
                    .sentAt(now.plus(Duration.ofDays( i )))
                    .recipientIds(List.of( "internalIdPF"+i ) )
                    .build() );
        }

        for(int i = 0;i<5;i++)
        {
            entityResultPG.getResults().add( NotificationMetadataEntity.builder()
                    .senderIdRecipientId( "senderId##internalIdPG"+i )
                    .iunRecipientId("IUN##internalIdPG"+i )
                    .notificationStatus( NotificationStatus.VIEWED.getValue() )
                    .senderId( "senderId" )
                    .sentAt(now.plus( Duration.ofDays( i ) ).plusSeconds( i ))
                    .recipientIds(List.of( "internalIdPG"+i ) )
                    .build() );
        }
        entityResultPF.setLastEvaluatedKey(null);
        entityResultPG.setLastEvaluatedKey(null);

        this.inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .senderReceiverId( "senderId" )
                .startDate( Instant.now().minus(500, ChronoUnit.DAYS) )
                .endDate( Instant.now() )
                .filterId("externalId")
                .opaqueFilterIdPF( "internalIdPF123" )
                .opaqueFilterIdPG( "internalIdPG123" )
                .size( PAGE_SIZE )
                .build();

        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(inputSearchNotificationDto);

        this.notificationSearchMultiPageByPFAndPGOnly = new NotificationSearchMultiPageByPFAndPGOnly(notificationDao, entityToDtoNotificationMetadataMapper, inputSearchNotificationDto, null, cfg, dataVaultClient, indexNameAndPartitions);


        Mockito.when(notificationDao.searchForOneMonth(inputSearchNotificationDto, indexNameAndPartitions.getIndexName().getValue(), indexNameAndPartitions.getPartitions().get(0), 124, null))
                .thenReturn(entityResultPF);
        Mockito.when(notificationDao.searchForOneMonth(inputSearchNotificationDto, indexNameAndPartitions.getIndexName().getValue(), indexNameAndPartitions.getPartitions().get(1), 124, null))
                .thenReturn(entityResultPG);

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 3 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchMultiPageByPFAndPGOnly.searchNotificationMetadata();

        Assertions.assertNotNull( result );
        Assertions.assertEquals(PAGE_SIZE, result.getResultsPage().size());
        Assertions.assertEquals(1, result.getNextPagesKey().size());
        Assertions.assertFalse(result.isMoreResult());
    }

}
