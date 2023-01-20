package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.DelegateType;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
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
        // TODO mock data vault

        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectDelegatedIndexAndPartitions(searchDto);
        searchMultiPage = new NotificationDelegatedSearchMultiPage(notificationDao, entityToDtoMapper, searchDto, null, cfg, dataVaultClient, mandateClient, indexNameAndPartitions);
    }

    @Test
    void testCheckMandate() {
        PageSearchTrunk<NotificationDelegationMetadataEntity> pst = new PageSearchTrunk<>();
        pst.setResults(List.of(
                NotificationDelegationMetadataEntity.builder()
                        .iunRecipientIdDelegateIdGroupId("N1").mandateId("m1")
                        .senderId("s1").recipientId("r1").sentAt(Instant.now())
                        .build(),
                NotificationDelegationMetadataEntity.builder()
                        .iunRecipientIdDelegateIdGroupId("N2").mandateId("m2")
                        .senderId("s1").recipientId("r2").sentAt(Instant.now())
                        .build(),
                NotificationDelegationMetadataEntity.builder()
                        .iunRecipientIdDelegateIdGroupId("N3").mandateId("m1")
                        .senderId("s2").recipientId("r1").sentAt(Instant.now())
                        .build(),
                NotificationDelegationMetadataEntity.builder()
                        .iunRecipientIdDelegateIdGroupId("N4").mandateId("m1")
                        .senderId("s1").recipientId("r1").sentAt(Instant.now().plus(200, ChronoUnit.DAYS))
                        .build(),
                NotificationDelegationMetadataEntity.builder()
                        .iunRecipientIdDelegateIdGroupId("N5").mandateId("m1")
                        .senderId("s1").recipientId("r1").sentAt(Instant.now().minus(200,  ChronoUnit.DAYS))
                        .build()
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

}