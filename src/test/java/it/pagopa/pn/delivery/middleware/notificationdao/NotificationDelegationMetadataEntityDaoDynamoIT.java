package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.LocalStackTestConfig;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.svc.search.IndexNameAndPartitions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        NotificationDelegationMetadataEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "pn.delivery.notification-dao.table-name=Notifications",
        "pn.delivery.notification-metadata-dao.table-name=NotificationsMetadata",
        "pn.delivery.notification-delegation-metadata-dao.table-name=NotificationDelegationMetadata"
})
@SpringBootTest
@Import(LocalStackTestConfig.class)
class NotificationDelegationMetadataEntityDaoDynamoIT {

    @Autowired
    private NotificationDelegationMetadataEntityDao entityDao;

    @MockBean
    private PnDataVaultClientImpl dataVaultClient;

    private static final String OPAQUE_TAX_ID_R1 = "TFZMREFBODVUNTBHNzAyQg==";
    private static final String TAX_ID_R1 = "LVLDAA85T50G702B";
    private static final String DENOMINATION_R1 = "Ada Lovelace";
    private static final String OPAQUE_TAX_ID_R2 = "Q0xNQ1NUNDJSMTJEOTY5Wg==";
    private static final String TAX_ID_R2 = "CLMCST42R12D969Z";
    private static final String DENOMINATION_R2 = "Cristoforo Colombo";

    @Test
    void testSearchDelegatedByMandateId() {
        PageSearchTrunk<NotificationDelegationMetadataEntity> result = entityDao.searchDelegatedByMandateId("mandateId", 10, null);
        assertNotNull(result);
    }

    @Test
    void testSearchForOneMonth() {
        InputSearchNotificationDelegatedDto searchDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId("delegateId")
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .endDate(Instant.now())
                .size(10)
                .build();

        when(dataVaultClient.getRecipientDenominationByInternalId(anyList()))
                .thenReturn(getDataVaultResults());

        PageSearchTrunk<NotificationDelegationMetadataEntity> result = entityDao.searchForOneMonth(searchDto,
                IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_DELEGATE, "delegateId##202301", searchDto.getSize(), null);
        assertNotNull(result);
    }

    @Test
    void testSearchForOneMonthWithFilters() {
        InputSearchNotificationDelegatedDto searchDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId("delegateId")
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .endDate(Instant.now())
                .statuses(List.of(NotificationStatus.ACCEPTED))
                .senderId("senderId")
                .receiverId("receiverId")
                .size(10)
                .build();

        when(dataVaultClient.getRecipientDenominationByInternalId(anyList()))
                .thenReturn(getDataVaultResults());

        PageSearchTrunk<NotificationDelegationMetadataEntity> result = entityDao.searchForOneMonth(searchDto,
                IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_DELEGATE, "delegateId##202301", searchDto.getSize(), null);
        assertNotNull(result);
    }

    @Test
    void testSearchForOneMonthWithResults() {
        NotificationDelegationMetadataEntity entity1 = newEntity("1");
        entity1.setDelegateIdCreationMonth("delegateId##202301");
        entityDao.putIfAbsent(entity1);
        NotificationDelegationMetadataEntity entity2 = newEntity("2");
        entity1.setDelegateIdCreationMonth("x##202301");
        entityDao.putIfAbsent(entity2);

        InputSearchNotificationDelegatedDto searchDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId("delegateId")
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .size(10)
                .build();

        when(dataVaultClient.getRecipientDenominationByInternalId(anyList()))
                .thenReturn(getDataVaultResults());

        PageSearchTrunk<NotificationDelegationMetadataEntity> result = entityDao.searchForOneMonth(searchDto,
                IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_DELEGATE, "delegateId##202301", searchDto.getSize(), null);
        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertTrue(result.getResults().stream().anyMatch(e -> e.getIunRecipientIdDelegateIdGroupId().equals("1")));
    }

    @Test
    void testPutIfAbsent() {
        NotificationDelegationMetadataEntity entity = newEntity("id");
        Key key = Key.builder()
                .partitionValue(entity.getIunRecipientIdDelegateIdGroupId())
                .sortValue(entity.getSentAt().toString())
                .build();
        entityDao.putIfAbsent(entity);
        Optional<NotificationDelegationMetadataEntity> fromDb = entityDao.get(key);
        assertTrue(fromDb.isPresent());
        assertEquals(fromDb.get(), entity);
    }

    @Test
    void testBatchPutItems() {
        List<NotificationDelegationMetadataEntity> entities = IntStream.range(0, 28)
                .mapToObj(i -> newEntity("iun" + i))
                .toList();
        List<NotificationDelegationMetadataEntity> unprocessed = entityDao.batchPutItems(entities);
        assertTrue(unprocessed.isEmpty());
    }

    @Test
    void testEmptyBatchPutItems() {
        List<NotificationDelegationMetadataEntity> unprocessed = entityDao.batchPutItems(Collections.emptyList());
        assertTrue(unprocessed.isEmpty());
    }

    @Test
    void testBatchDeleteItems() {
        List<NotificationDelegationMetadataEntity> entities = IntStream.range(0, 28)
                .mapToObj(i -> newEntity("iun" + i))
                .toList();
        List<NotificationDelegationMetadataEntity> unprocessed = entityDao.batchDeleteItems(entities);
        assertTrue(unprocessed.isEmpty());
    }

    @Test
    void testEmptyBatchDeleteItems() {
        List<NotificationDelegationMetadataEntity> unprocessed = entityDao.batchDeleteItems(Collections.emptyList());
        assertTrue(unprocessed.isEmpty());
    }

    @Test
    void testDeleteWithConditions() {
        NotificationDelegationMetadataEntity entity = newEntity("iun");
        entity.setMandateId("mandateId");
        entityDao.putIfAbsent(entity);

        Key key = Key.builder()
                .partitionValue(entity.getIunRecipientIdDelegateIdGroupId())
                .sortValue(entity.getSentAt().toString())
                .build();

        entity.setMandateId("X");
        assertFalse(entityDao.deleteWithConditions(entity).isPresent());
        assertTrue(entityDao.get(key).isPresent());

        entity.setMandateId("mandateId");
        assertTrue(entityDao.deleteWithConditions(entity).isPresent());
        assertFalse(entityDao.get(key).isPresent());

        assertFalse(entityDao.deleteWithConditions(entity).isPresent());
    }

    private NotificationDelegationMetadataEntity newEntity(String iun) {
        return NotificationDelegationMetadataEntity.builder()
                .iunRecipientIdDelegateIdGroupId(iun)
                .sentAt(Instant.now())
                .build();
    }

    private List<BaseRecipientDto> getDataVaultResults() {
        return List.of(
                new BaseRecipientDto()
                        .internalId(OPAQUE_TAX_ID_R1)
                        .taxId(TAX_ID_R1)
                        .denomination(DENOMINATION_R1)
                        .recipientType(RecipientType.PF),
                new BaseRecipientDto()
                        .internalId(OPAQUE_TAX_ID_R2)
                        .taxId(TAX_ID_R2)
                        .denomination(DENOMINATION_R2)
                        .recipientType(RecipientType.PF)
        );
    }
}
