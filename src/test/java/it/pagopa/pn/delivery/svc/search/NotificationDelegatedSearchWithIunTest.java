package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.DelegateType;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class NotificationDelegatedSearchWithIunTest {

    private static final int PAGE_SIZE = 10;

    private static final String DELEGATE_ID = "delegateId";

    private NotificationDao notificationDao;
    private PnDeliveryConfigs cfg;
    private NotificationDelegatedSearchUtils notificationDelegatedSearchUtils;
    private PnMandateClientImpl mandateClient;
    private NotificationDelegatedSearchWithIun searchMultiPage;
    private EntityToDtoNotificationMetadataMapper entityToDtoMapper;


    @BeforeEach
    void setup() {
        notificationDao = mock(NotificationDao.class);
        cfg = mock(PnDeliveryConfigs.class);
        entityToDtoMapper = mock(EntityToDtoNotificationMetadataMapper.class);
        PnDataVaultClientImpl dataVaultClient = mock(PnDataVaultClientImpl.class);
        mandateClient = mock(PnMandateClientImpl.class);
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
        searchMultiPage = new NotificationDelegatedSearchWithIun(notificationDao, entityToDtoMapper, searchDto, null, cfg, dataVaultClient, notificationDelegatedSearchUtils, indexNameAndPartitions);
    }

    @Test
    void testSearchNotificationMetadata() {
        PageSearchTrunk<NotificationDelegationMetadataEntity> pst = new PageSearchTrunk<>();
        pst.setResults(List.of(
                generateNotificationDelegationMetadataEntity("N1", "m1", "s1", "r1", null),
                generateNotificationDelegationMetadataEntity("N2", "m2", "s1", "r2", null),
                generateNotificationDelegationMetadataEntity("N3", "m1", "s2", "r3", null),
                generateNotificationDelegationMetadataEntity("N4", "m1", "s1", "r4", Instant.now().plus(200, ChronoUnit.DAYS)),
                generateNotificationDelegationMetadataEntity("N5", "m1", "s1", "r5", Instant.now().minus(200,  ChronoUnit.DAYS))
        ));
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setIun("N1");
        internalNotification.setRecipientIds(List.of("r1", "r2", "r3","r4","r5"));
        when(notificationDao.getNotificationByIun(any(), anyBoolean())).thenReturn(Optional.of(internalNotification));
        when(notificationDao.searchDelegatedForOneMonth(any(), any(), any(), anyInt(), any()))
                .thenReturn(pst).thenReturn(pst);

        NotificationDelegationMetadataEntity entity = new NotificationDelegationMetadataEntity();

        when(notificationDao.searchByPk(any())).thenReturn(Page.create(Collections.singletonList(entity), null));
        when(cfg.getMaxPageSize()).thenReturn(1);
        when(cfg.getMaxPageSize()).thenReturn(1);
        when(notificationDao.searchDelegatedForOneMonth(any(), any(), any(), anyInt(), any()))
                .thenReturn(pst).thenReturn(pst);
        when(cfg.getMaxPageSize()).thenReturn(1);
        InternalMandateDto mandate1 = new InternalMandateDto();
        mandate1.setMandateId("m1");
        mandate1.setDelegator("r1");
        mandate1.setDelegate(DELEGATE_ID);
        mandate1.setDatefrom(Instant.now().minus(100, ChronoUnit.DAYS).toString());
        mandate1.setDateto(Instant.now().plus(100, ChronoUnit.DAYS).toString());
        mandate1.setVisibilityIds(List.of("s1"));
        when(notificationDelegatedSearchUtils.checkMandates(any(), any())).thenReturn(pst.getResults());
        when(mandateClient.listMandatesByDelegators(eq(DelegateType.PG), any(), anyList()))
                .thenReturn(List.of(mandate1));
        NotificationSearchRow notificationSearchRow = new NotificationSearchRow();
        notificationSearchRow.setRecipients(List.of("r1"));
        when(entityToDtoMapper.entity2Dto((NotificationDelegationMetadataEntity) any()))
                .thenReturn(notificationSearchRow);
        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = searchMultiPage.searchNotificationMetadata();
        assertNotNull(result);
        assertEquals(5, result.getResultsPage().size());
    }

    private NotificationDelegationMetadataEntity generateNotificationDelegationMetadataEntity(String pk, String mandateId, String senderId, String recipientId, Instant sentAt) {
        return NotificationDelegationMetadataEntity.builder()
                .iunRecipientIdDelegateIdGroupId(pk).mandateId(mandateId)
                .senderId(senderId).recipientId(recipientId)
                .sentAt(sentAt == null ? Instant.now() : sentAt)
                .build();
    }

}