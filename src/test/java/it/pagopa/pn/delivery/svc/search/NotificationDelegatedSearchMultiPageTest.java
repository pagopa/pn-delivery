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


class NotificationDelegatedSearchMultiPageTest {

    private static final int PAGE_SIZE = 10;

    private static final String DELEGATE_ID = "delegateId";

    private NotificationDao notificationDao;
    private InputSearchNotificationDelegatedDto searchDto;
    private PnDeliveryConfigs cfg;
    private NotificationDelegatedSearchMultiPage searchMultiPage;
    private NotificationDelegatedSearchUtils notificationDelegatedSearchUtils;
    private EntityToDtoNotificationMetadataMapper entityToDtoMapper;

    @BeforeEach
    void setup() {
        notificationDao = mock(NotificationDao.class);
        cfg = mock(PnDeliveryConfigs.class);
        entityToDtoMapper = mock(EntityToDtoNotificationMetadataMapper.class);
        PnDataVaultClientImpl dataVaultClient = mock(PnDataVaultClientImpl.class);
        notificationDelegatedSearchUtils = mock(NotificationDelegatedSearchUtils.class);
        searchDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(Instant.now().minus(500, ChronoUnit.DAYS))
                .endDate(Instant.now())
                .size(PAGE_SIZE)
                .build();
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(NotificationSearchRow.builder().build());

        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectDelegatedIndexAndPartitions(searchDto);
        searchMultiPage = new NotificationDelegatedSearchMultiPage(notificationDao, entityToDtoMapper, searchDto, null, cfg, dataVaultClient, notificationDelegatedSearchUtils, indexNameAndPartitions);
    }

    @Test
    void testCheckMandate() {
        PageSearchTrunk<NotificationDelegationMetadataEntity> pst = new PageSearchTrunk<>();
        pst.setResults(List.of(
                generateNotificationDelegationMetadataEntity("N1", "m1", "s1", "r1", null),
                generateNotificationDelegationMetadataEntity("N2", "m2", "s1", "r2", null),
                generateNotificationDelegationMetadataEntity("N3", "m1", "s2", "r1", null),
                generateNotificationDelegationMetadataEntity("N4", "m1", "s1", "r1", Instant.now().plus(200, ChronoUnit.DAYS)),
                generateNotificationDelegationMetadataEntity("N5", "m1", "s1", "r1", Instant.now().minus(200,  ChronoUnit.DAYS))
        ));
        when(notificationDao.searchDelegatedForOneMonth(any(), any(), any(), anyInt(), any()))
                .thenReturn(pst).thenReturn(pst);
        when(cfg.getMaxPageSize()).thenReturn(1);
        when(notificationDelegatedSearchUtils.checkMandates(any(), any())).thenReturn(pst.getResults());
        NotificationSearchRow notificationSearchRow = new NotificationSearchRow();
        notificationSearchRow.setRecipients(List.of("r1"));
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(notificationSearchRow);
        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = searchMultiPage.searchNotificationMetadata();
        assertNotNull(result);
        assertEquals(10, result.getResultsPage().size());
    }

    @Test
    void testMultiPageSearch() {
        List<NotificationDelegationMetadataEntity> listTrunk1 = List.of(
                generateNotificationDelegationMetadataEntity("N1", "m1", "s", "r", null),
                generateNotificationDelegationMetadataEntity("N2", "m1", "s", "r", null),
                generateNotificationDelegationMetadataEntity("N3", "m1", "s", "r", null));
        PageSearchTrunk<NotificationDelegationMetadataEntity> trunk1 = new PageSearchTrunk<>();
        trunk1.setResults(listTrunk1);
        trunk1.setLastEvaluatedKey(Collections.emptyMap());

        List<NotificationDelegationMetadataEntity> listTrunk2 = List.of(
                generateNotificationDelegationMetadataEntity("N4", "m1", "s", "r", null),
                generateNotificationDelegationMetadataEntity("N5", "m1", "s", "r", null),
                generateNotificationDelegationMetadataEntity("N6", "m1", "s", "r", null));
        PageSearchTrunk<NotificationDelegationMetadataEntity> trunk2 = new PageSearchTrunk<>();
        trunk2.setResults(listTrunk2);

        when(notificationDao.searchDelegatedForOneMonth(any(), any(), any(), anyInt(), any()))
                .thenReturn(trunk1).thenReturn(trunk2);
        when(cfg.getMaxPageSize()).thenReturn(1);

        when(notificationDelegatedSearchUtils.checkMandates(any(), any())).thenReturn(listTrunk1).thenReturn(listTrunk2);

        NotificationSearchRow notificationSearchRow = new NotificationSearchRow();
        notificationSearchRow.setRecipients(List.of("r1"));
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(notificationSearchRow);

        searchDto.setSize(3);

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = searchMultiPage.searchNotificationMetadata();
        assertNotNull(result);
        assertEquals(3, result.getResultsPage().size());
        // nella prima iterazione recupero due record, ma ne scarto uno per la delega, alla seconda iterazione trovo il secondo
    }

    private NotificationDelegationMetadataEntity generateNotificationDelegationMetadataEntity(String pk, String mandateId, String senderId, String recipientId, Instant sentAt) {
        return NotificationDelegationMetadataEntity.builder()
                .iunRecipientIdDelegateIdGroupId(pk).mandateId(mandateId)
                .senderId(senderId).recipientId(recipientId)
                .sentAt(sentAt == null ? Instant.now() : sentAt)
                .build();
    }

}