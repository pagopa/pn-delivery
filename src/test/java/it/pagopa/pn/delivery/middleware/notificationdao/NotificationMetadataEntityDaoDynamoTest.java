package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {PnDeliveryConfigs.class})
@ExtendWith(SpringExtension.class)
class NotificationMetadataEntityDaoDynamoTest {
    @MockBean
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @MockBean
    private NotificationMetadataEntityDaoDynamo notificationMetadataEntityDaoDynamo;

    @Autowired
    private PnDeliveryConfigs pnDeliveryConfigs;

    public static final String SENDER_DENOMINATION = "SenderId";
    private NotificationMetadataEntityDao metadataEntityDao;


    /**
     * Method under test: {@link NotificationMetadataEntityDaoDynamo#searchByIun(InputSearchNotificationDto, String, String)}
     */
    @Test
    void testSearchByIun() {
        PageSearchTrunk<NotificationMetadataEntity> pageSearchTrunk = new PageSearchTrunk<>();
        when(notificationMetadataEntityDaoDynamo.searchByIun(Mockito.any(),
                Mockito.any(), Mockito.any())).thenReturn(pageSearchTrunk);
        assertSame(pageSearchTrunk,
                notificationMetadataEntityDaoDynamo.searchByIun(new InputSearchNotificationDto(), "42", "42"));
        verify(notificationMetadataEntityDaoDynamo).searchByIun(Mockito.any(),
                Mockito.any(), Mockito.any());
    }


    /**
     * Method under test: {@link NotificationMetadataEntityDaoDynamo#searchForOneMonth(InputSearchNotificationDto, String, String, int, PnLastEvaluatedKey)}
     */
    @Test
    void testSearchForOneMonth2() {
        PageSearchTrunk<NotificationMetadataEntity> pageSearchTrunk = new PageSearchTrunk<>();
        when(notificationMetadataEntityDaoDynamo.searchForOneMonth(Mockito.any(),
                Mockito.any(), Mockito.any(), anyInt(), Mockito.any()))
                .thenReturn(pageSearchTrunk);
        InputSearchNotificationDto inputSearchNotificationDto = mock(InputSearchNotificationDto.class);
        assertSame(pageSearchTrunk, notificationMetadataEntityDaoDynamo.searchForOneMonth(inputSearchNotificationDto,
                "Index Name", "42", 3, new PnLastEvaluatedKey()));
        verify(notificationMetadataEntityDaoDynamo).searchForOneMonth(Mockito.any(),
                Mockito.any(), Mockito.any(), anyInt(), Mockito.any());
    }

    @BeforeEach
    void setup() {
        metadataEntityDao = new NotificationMetadataEntityDaoDynamoTest.MetadataEntityDaoMock();
    }

    /**
     * Method under test: {@link NotificationMetadataEntityDaoDynamo#putIfAbsent(NotificationMetadataEntity)}
     */
    @Test
    void testPutIfAbsent() {
        doNothing().when(notificationMetadataEntityDaoDynamo).putIfAbsent(Mockito.any());
        notificationMetadataEntityDaoDynamo.putIfAbsent(new NotificationMetadataEntity());
        verify(notificationMetadataEntityDaoDynamo).putIfAbsent(Mockito.any());
    }

    @Test
    void InsertMetadataEntitySuccess() throws PnIdConflictException {
        //Given
        NotificationMetadataEntity entityToInsert = NotificationMetadataEntity.builder()
                .notificationGroup("Notification_Group")
                .notificationStatus(NotificationStatusV26.ACCEPTED.toString())
                .iunRecipientId("IUN##RecipientId")
                .recipientIdCreationMonth("RecipientId##creationMonth")
                .recipientIds(Collections.singletonList("RecipientId"))
                .recipientOne(true)
                .senderId("SenderId")
                .senderIdCreationMonth("SenderId##CreationMonth")
                .senderIdRecipientId("SenderId##RecipientId")
                .sentAt(Instant.parse("2022-04-06T17:48:00Z"))
                .tableRow(Map.ofEntries(
                        Map.entry("iun", "IUN"),
                        Map.entry("recipientsIds", Collections.singletonList("RecipientId").toString()),
                        Map.entry("paNotificationId", "PaNotificationId"),
                        Map.entry("subject", "Subject"),
                        Map.entry("senderDenomination", SENDER_DENOMINATION))
                )
                .recipientId("RecipientId")
                .build();

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .bySender(true)
                .senderReceiverId("SenderId")
                .startDate(Instant.parse("2022-04-01T17:48:00Z"))
                .endDate(Instant.parse("2022-04-30T17:48:00Z"))
                .size(10)
                .nextPagesKey(null)
                .build();

        //When
        metadataEntityDao.putIfAbsent(entityToInsert);

        //Then
        PageSearchTrunk<NotificationMetadataEntity> result = metadataEntityDao.searchForOneMonth(
                searchDto,
                "senderId",
                "SenderId##CreationMonth",
                10,
                null);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(SENDER_DENOMINATION, result.getResults().get(0).getSenderId());
    }

    private static class MetadataEntityDaoMock implements NotificationMetadataEntityDao {

        private final EntityToDtoNotificationMetadataMapper entityToDto = new EntityToDtoNotificationMetadataMapper();

        private final Map<Key, NotificationMetadataEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void put(NotificationMetadataEntity notificationMetadataEntity) {

        }

        @Override
        public void putIfAbsent(NotificationMetadataEntity notificationMetadataEntity) throws PnIdConflictException {
            Key key = Key.builder()
                    .partitionValue(notificationMetadataEntity.getIunRecipientId())
                    .sortValue(notificationMetadataEntity.getSentAt().toString())
                    .build();
            storage.put(key, notificationMetadataEntity);
        }

        @Override
        public Optional<NotificationMetadataEntity> get(Key key) {
            return Optional.of(storage.get(key));
        }

        @Override
        public void delete(Key key) {

        }


        @Override
        public PageSearchTrunk<NotificationMetadataEntity> searchByIun(InputSearchNotificationDto inputSearchNotificationDto, String partitionValue, String sentAt) {
            Key key = Key.builder()
                    .partitionValue("IUN##RecipientId")
                    .sortValue("2022-04-06T17:48:00Z")
                    .build();
            NotificationMetadataEntity getResult = storage.get(key);

            PageSearchTrunk<NotificationMetadataEntity> res = new PageSearchTrunk<>();
            res.setResults(Collections.singletonList(getResult));
            return res;
        }

        @Override
        public PageSearchTrunk<NotificationMetadataEntity> searchForOneMonth(InputSearchNotificationDto inputSearchNotificationDto, String indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {
            Key key = Key.builder()
                    .partitionValue("IUN##RecipientId")
                    .sortValue("2022-04-06T17:48:00Z")
                    .build();
            NotificationMetadataEntity getResult = storage.get(key);

            PageSearchTrunk<NotificationMetadataEntity> res = new PageSearchTrunk<>();
            res.setResults(Collections.singletonList(getResult));
            return res;
        }
    }

}