package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class NotificationDelegatedSearchExactTest {

    private static final int PAGE_SIZE = 10;

    private static final String DELEGATE_ID = "delegateId";

    private NotificationDao notificationDao;
    private PnDeliveryConfigs cfg;
    private NotificationDelegatedSearchExact searchMultiPage;
    private EntityToDtoNotificationMetadataMapper entityToDtoMapper;

    private NotificationDelegatedSearchUtils notificationDelegatedSearchUtils;

    @BeforeEach
    void setup() {
        notificationDao = mock(NotificationDao.class);
        cfg = mock(PnDeliveryConfigs.class);
        entityToDtoMapper = mock(EntityToDtoNotificationMetadataMapper.class);
        PnDataVaultClientImpl dataVaultClient = mock(PnDataVaultClientImpl.class);
        notificationDelegatedSearchUtils = mock(NotificationDelegatedSearchUtils.class);
        InputSearchNotificationDelegatedDto searchDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(Instant.now().minus(500, ChronoUnit.DAYS))
                .endDate(Instant.now())
                .size(PAGE_SIZE)
                .build();
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(NotificationSearchRow.builder().build());

        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectDelegatedIndexAndPartitions(searchDto);
        searchMultiPage = new NotificationDelegatedSearchExact(notificationDao, entityToDtoMapper, searchDto, cfg, dataVaultClient, notificationDelegatedSearchUtils, indexNameAndPartitions);
    }

    @Test
    void testSearchNotificationMetadata() {
        PageSearchTrunk<NotificationDelegationMetadataEntity> pst = new PageSearchTrunk<>();
        pst.setResults(List.of(
                generateNotificationDelegationMetadataEntity()
         ));
        when(notificationDao.searchDelegatedForOneMonth(any(), any(), any(), anyInt(), any()))
                .thenReturn(pst).thenReturn(pst);

        NotificationDelegationMetadataEntity entity = new NotificationDelegationMetadataEntity();
        entity.setMandateId("m1");
        entity.setRecipientId("r1");
        entity.setSentAt(Instant.now());
        entity.setSenderId("s1");
        Page<NotificationDelegationMetadataEntity> page = Page.create(Collections.singletonList(entity), null);
        when(notificationDao.searchByPk(any())).thenReturn(page);
        when(cfg.getMaxPageSize()).thenReturn(1);
        when(notificationDelegatedSearchUtils.checkMandates(any(), any())).thenReturn(pst.getResults());
        NotificationSearchRow notificationSearchRow = new NotificationSearchRow();
        notificationSearchRow.setRecipients(List.of("r1"));
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(notificationSearchRow);
        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = searchMultiPage.searchNotificationMetadata();
        assertNotNull(result);
        assertEquals(1, result.getResultsPage().size());
    }

    private NotificationDelegationMetadataEntity generateNotificationDelegationMetadataEntity() {
        return NotificationDelegationMetadataEntity.builder()
                .iunRecipientIdDelegateIdGroupId("pk").mandateId("m1")
                .senderId("s1").recipientId("r1")
                .sentAt(Instant.now())
                .build();
    }

}