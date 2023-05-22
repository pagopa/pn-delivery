package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.DelegateType;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


class NotificationDelegatedSearchMultiPageTest {

    private static final int PAGE_SIZE = 10;

    private static final String DELEGATE_ID = "delegateId";

    private NotificationDao notificationDao;
    private InputSearchNotificationDelegatedDto searchDto;
    private PnDeliveryConfigs cfg;
    private PnDataVaultClientImpl dataVaultClient;
    private PnMandateClientImpl mandateClient;
    private NotificationDelegatedSearchMultiPage searchMultiPage;
    private EntityToDtoNotificationMetadataMapper entityToDtoMapper;

    private int nPartitions;

    @BeforeEach
    void setup() {
        notificationDao = mock(NotificationDao.class);
        cfg = mock(PnDeliveryConfigs.class);
        entityToDtoMapper = mock(EntityToDtoNotificationMetadataMapper.class);
        dataVaultClient = mock(PnDataVaultClientImpl.class);
        mandateClient = mock(PnMandateClientImpl.class);
        searchDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(Instant.now().minus(500, ChronoUnit.DAYS))
                .endDate(Instant.now())
                .size(PAGE_SIZE)
                .build();
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(NotificationSearchRow.builder().build());

        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectDelegatedIndexAndPartitions(searchDto);
        nPartitions = indexNameAndPartitions.getPartitions().size();
        searchMultiPage = new NotificationDelegatedSearchMultiPage(notificationDao, entityToDtoMapper, searchDto, null, cfg, dataVaultClient, mandateClient, indexNameAndPartitions);
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
                .thenReturn(pst).thenReturn(new PageSearchTrunk<>());
        when(cfg.getMaxPageSize()).thenReturn(1);
        InternalMandateDto mandate1 = new InternalMandateDto();
        mandate1.setMandateId("m1");
        mandate1.setDelegator("r1");
        mandate1.setDelegate(DELEGATE_ID);
        mandate1.setDatefrom(Instant.now().minus(100, ChronoUnit.DAYS).toString());
        mandate1.setDateto(Instant.now().plus(100, ChronoUnit.DAYS).toString());
        mandate1.setVisibilityIds(List.of("s1"));
        when(mandateClient.listMandatesByDelegators(eq(DelegateType.PG), any(), anyList()))
                .thenReturn(List.of(mandate1));
        NotificationSearchRow notificationSearchRow = new NotificationSearchRow();
        notificationSearchRow.setRecipients(List.of("r1"));
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(notificationSearchRow);
        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = searchMultiPage.searchNotificationMetadata();
        assertNotNull(result);
        assertEquals(1, result.getResultsPage().size());
        verify(entityToDtoMapper).entity2Dto(pst.getResults().get(0));
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

        InternalMandateDto mandate = new InternalMandateDto();
        mandate.setMandateId("m1");
        mandate.setDelegator("r");
        mandate.setDelegate(DELEGATE_ID);
        when(mandateClient.listMandatesByDelegators(eq(DelegateType.PG), any(), anyList()))
                .thenReturn(List.of(mandate));
        NotificationSearchRow notificationSearchRow = new NotificationSearchRow();
        notificationSearchRow.setRecipients(List.of("r1"));
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(notificationSearchRow);

        searchDto.setSize(3);

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = searchMultiPage.searchNotificationMetadata();
        assertNotNull(result);
        assertEquals(3, result.getResultsPage().size());
        // nella prima iterazione recupero due record, ma ne scarto uno per la delega, alla seconda iterazione trovo il secondo
        verify(notificationDao, times(2)).searchDelegatedForOneMonth(any(), any(), any(), anyInt(), any());
    }

    @Test
    void testMultipleIterationReachRequestedSize() {
        List<NotificationDelegationMetadataEntity> listTrunk1 = List.of(
                generateNotificationDelegationMetadataEntity("N1", "m2", "s", "r", null),
                generateNotificationDelegationMetadataEntity("N2", "m1", "s", "r", null),
                generateNotificationDelegationMetadataEntity("N3", "m1", "s", "r", null));
        PageSearchTrunk<NotificationDelegationMetadataEntity> trunk1 = new PageSearchTrunk<>();
        trunk1.setResults(listTrunk1);

        List<NotificationDelegationMetadataEntity> listTrunk2 = List.of(
                generateNotificationDelegationMetadataEntity("N4", "m2", "s", "r", null),
                generateNotificationDelegationMetadataEntity("N5", "m2", "s", "r", null),
                generateNotificationDelegationMetadataEntity("N6", "m1", "s", "r", null));
        PageSearchTrunk<NotificationDelegationMetadataEntity> trunk2 = new PageSearchTrunk<>();
        trunk2.setResults(listTrunk2);

        when(notificationDao.searchDelegatedForOneMonth(any(), any(), any(), anyInt(), any()))
                .thenReturn(trunk1).thenReturn(trunk2);
        when(cfg.getMaxPageSize()).thenReturn(1);

        InternalMandateDto mandate = new InternalMandateDto();
        mandate.setMandateId("m2");
        mandate.setDelegator("r");
        mandate.setDelegate(DELEGATE_ID);
        when(mandateClient.listMandatesByDelegators(eq(DelegateType.PG), any(), anyList()))
                .thenReturn(List.of(mandate));
        NotificationSearchRow notificationSearchRow = new NotificationSearchRow();
        notificationSearchRow.setRecipients(List.of("r1"));
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(notificationSearchRow);

        searchDto.setSize(2);

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = searchMultiPage.searchNotificationMetadata();
        assertNotNull(result);
        assertEquals(2, result.getResultsPage().size());
        // nella prima iterazione recupero due record, ma ne scarto uno per la delega, alla seconda iterazione trovo il secondo
        verify(notificationDao, times(2)).searchDelegatedForOneMonth(any(), any(), any(), anyInt(), any());
    }

    @Test
    void testMultipleIterationNoMoreData() {
        List<NotificationDelegationMetadataEntity> listTrunk1 = List.of(
                generateNotificationDelegationMetadataEntity("N1", "m2", "s", "r", null),
                generateNotificationDelegationMetadataEntity("N2", "m1", "s", "r", null));
        PageSearchTrunk<NotificationDelegationMetadataEntity> trunk1 = new PageSearchTrunk<>();
        trunk1.setResults(listTrunk1);

        List<NotificationDelegationMetadataEntity> listTrunk2 = List.of(
                generateNotificationDelegationMetadataEntity("N3", "m1", "s", "r", null),
                generateNotificationDelegationMetadataEntity("N4", "m1", "s", "r", null));
        PageSearchTrunk<NotificationDelegationMetadataEntity> trunk2 = new PageSearchTrunk<>();
        trunk2.setResults(listTrunk2);

        when(notificationDao.searchDelegatedForOneMonth(any(), any(), any(), anyInt(), any()))
                .thenReturn(trunk1).thenReturn(trunk2).thenReturn(new PageSearchTrunk<>());
        when(cfg.getMaxPageSize()).thenReturn(1);

        InternalMandateDto mandate = new InternalMandateDto();
        mandate.setMandateId("m2");
        mandate.setDelegator("r");
        mandate.setDelegate(DELEGATE_ID);
        when(mandateClient.listMandatesByDelegators(eq(DelegateType.PG), any(), anyList()))
                .thenReturn(List.of(mandate));
        NotificationSearchRow notificationSearchRow = new NotificationSearchRow();
        notificationSearchRow.setRecipients(List.of("r1"));
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(notificationSearchRow);

        searchDto.setSize(1);

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = searchMultiPage.searchNotificationMetadata();
        assertNotNull(result);
        assertEquals(1, result.getResultsPage().size());
        // nella prima iterazione recupero due record, ma ne scarto uno per la delega, alla seconda iterazione scarto tutti i record, ma non ho più niente in tabella
        verify(notificationDao, times(nPartitions + 2)).searchDelegatedForOneMonth(any(), any(), any(), anyInt(), any());
    }

    private NotificationDelegationMetadataEntity generateNotificationDelegationMetadataEntity(String pk, String mandateId, String senderId, String recipientId, Instant sentAt) {
        return NotificationDelegationMetadataEntity.builder()
                .iunRecipientIdDelegateIdGroupId(pk).mandateId(mandateId)
                .senderId(senderId).recipientId(recipientId)
                .sentAt(sentAt == null ? Instant.now() : sentAt)
                .build();
    }

}