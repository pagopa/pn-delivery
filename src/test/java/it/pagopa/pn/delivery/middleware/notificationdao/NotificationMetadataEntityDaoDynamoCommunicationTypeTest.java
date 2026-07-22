package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.NotificationSearchCommunicationType;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test mirati al filtro per {@code communicationType} introdotto in WI-US2.5:
 * <ul>
 *     <li>{@code searchForOneMonth} → FilterExpression generata (query multi-mese);</li>
 *     <li>{@code searchByIun} → filtro applicato in memoria (GetItem puntuale).</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
class NotificationMetadataEntityDaoDynamoCommunicationTypeTest {

    private static final String TABLE_NAME = "NotificationsMetadata";
    private static final String INDEX_NAME = NotificationMetadataEntity.FIELD_RECIPIENT_ID;
    private static final String SENDER_INDEX_NAME = NotificationMetadataEntity.FIELD_SENDER_ID;
    private static final String PARTITION = "PF-recipient##202209";
    private static final String SENDER_PARTITION = "PA-sender##202209";
    private static final String RECIPIENT_ID = "PF-recipient";
    private static final String SENDER_ID = "PA-sender";
    private static final Instant SENT_AT = Instant.parse("2022-09-05T18:47:39.267123Z");
    private static final Instant START_DATE = Instant.parse("2022-09-01T00:00:00.00Z");
    private static final Instant END_DATE = Instant.parse("2022-09-30T00:00:00.00Z");

    private DynamoDbTable<NotificationMetadataEntity> table;
    private DynamoDbIndex<NotificationMetadataEntity> index;
    private NotificationMetadataEntityDaoDynamo dao;

    @BeforeEach
    void setup() {
        DynamoDbEnhancedClient enhancedClient = mock(DynamoDbEnhancedClient.class);
        table = mock(DynamoDbTable.class);
        index = mock(DynamoDbIndex.class);

        when(enhancedClient.table(eq(TABLE_NAME), any(TableSchema.class))).thenReturn(table);
        when(table.index(anyString())).thenReturn(index);

        PnDeliveryConfigs cfg = new PnDeliveryConfigs();
        PnDeliveryConfigs.NotificationMetadataDao metadataDao = new PnDeliveryConfigs.NotificationMetadataDao();
        metadataDao.setTableName(TABLE_NAME);
        cfg.setNotificationMetadataDao(metadataDao);

        dao = new NotificationMetadataEntityDaoDynamo(enhancedClient, cfg);
    }

    // ---------- searchForOneMonth: FilterExpression ----------

    @Test
    void searchForOneMonthLegalAppliesLegalOrAttributeNotExists() {
        Expression filter = captureFilterExpression(NotificationSearchCommunicationType.LEGAL);

        assertNotNull(filter.expression());
        assertTrue(filter.expression().contains(NotificationMetadataEntity.FIELD_COMMUNICATION_TYPE + " = :legal"),
                "Deve filtrare per communicationType = :legal");
        assertTrue(filter.expression().contains("attribute_not_exists(" + NotificationMetadataEntity.FIELD_COMMUNICATION_TYPE + ")"),
                "Deve includere le notifiche storiche prive del campo");
        assertEquals("LEGAL", filter.expressionValues().get(":legal").s());
        assertFalse(filter.expressionValues().containsKey(":informal"));
    }

    @Test
    void searchForOneMonthInformalAppliesInformalEquality() {
        Expression filter = captureFilterExpression(NotificationSearchCommunicationType.INFORMAL);

        assertNotNull(filter.expression());
        assertTrue(filter.expression().contains(NotificationMetadataEntity.FIELD_COMMUNICATION_TYPE + " = :informal"));
        assertFalse(filter.expression().contains("attribute_not_exists"),
                "INFORMAL non deve includere i record privi del campo");
        assertEquals("INFORMAL", filter.expressionValues().get(":informal").s());
        assertFalse(filter.expressionValues().containsKey(":legal"));
    }

    @Test
    void searchForOneMonthAllDoesNotApplyCommunicationTypeFilter() {
        Expression filter = captureFilterExpression(NotificationSearchCommunicationType.ALL);

        // nessun altro filtro impostato → expression nulla
        assertNull(filter.expression());
    }

    @Test
        void searchForOneMonthNullCommunicationTypeThrowsException() {
        InputSearchNotificationDto searchDto = baseReceiverSearch()
            .communicationType(null)
            .build();

        assertThrows(PnInternalException.class,
            () -> dao.searchForOneMonth(searchDto, INDEX_NAME, PARTITION, 10, null));
        verify(index, never()).query(any(QueryEnhancedRequest.class));
        }

        @Test
        void searchByIunNullCommunicationTypeThrowsException() {
        InputSearchNotificationDto searchDto = baseReceiverSearch()
            .communicationType(null)
            .build();

        assertThrows(PnInternalException.class,
            () -> dao.searchByIun(searchDto, PARTITION, SENT_AT.toString()));
        verify(table, never()).getItem(any(software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest.class));
    }

    private Expression captureFilterExpression(NotificationSearchCommunicationType communicationType) {
        Page<NotificationMetadataEntity> page = Page.create(Collections.emptyList());
        when(index.query(any(QueryEnhancedRequest.class))).thenReturn(() -> List.of(page).iterator());

        InputSearchNotificationDto searchDto = baseReceiverSearch()
                .communicationType(communicationType)
                .build();

        dao.searchForOneMonth(searchDto, INDEX_NAME, PARTITION, 10, null);

        ArgumentCaptor<QueryEnhancedRequest> captor = ArgumentCaptor.forClass(QueryEnhancedRequest.class);
        verify(index).query(captor.capture());
        return captor.getValue().filterExpression();
    }

    // ---------- searchByIun: filtro in memoria ----------

    @Test
    void searchByIunLegalKeepsLegalEntity() {
        mockGetItem(communicationTypeEntity("LEGAL"));
        assertResult(NotificationSearchCommunicationType.LEGAL, true);
    }

    @Test
    void searchByIunLegalKeepsHistoricalEntityWithoutCommunicationType() {
        mockGetItem(communicationTypeEntity(null));
        assertResult(NotificationSearchCommunicationType.LEGAL, true);
    }

    @Test
    void searchByIunLegalDiscardsInformalEntity() {
        mockGetItem(communicationTypeEntity("INFORMAL"));
        assertResult(NotificationSearchCommunicationType.LEGAL, false);
    }

    @Test
    void searchByIunInformalKeepsInformalEntity() {
        mockGetItem(communicationTypeEntity("INFORMAL"));
        assertResult(NotificationSearchCommunicationType.INFORMAL, true);
    }

    @Test
    void searchByIunInformalDiscardsLegalEntity() {
        mockGetItem(communicationTypeEntity("LEGAL"));
        assertResult(NotificationSearchCommunicationType.INFORMAL, false);
    }

    @Test
    void searchByIunInformalDiscardsHistoricalEntityWithoutCommunicationType() {
        mockGetItem(communicationTypeEntity(null));
        assertResult(NotificationSearchCommunicationType.INFORMAL, false);
    }

    @Test
    void searchByIunAllKeepsAnyEntity() {
        mockGetItem(communicationTypeEntity("INFORMAL"));
        assertResult(NotificationSearchCommunicationType.ALL, true);
    }

    @Test
    void searchByIunAllFilterKeepsAnyEntity() {
        mockGetItem(communicationTypeEntity("INFORMAL"));
        assertResult(NotificationSearchCommunicationType.ALL, true);
    }

    @Test
    void searchByIunReceiverKeepsRequestedSender() {
        mockGetItem(senderCommunicationTypeEntity("LEGAL"));

        InputSearchNotificationDto searchDto = baseReceiverSearch()
                .filterId(SENDER_ID)
                .communicationType(NotificationSearchCommunicationType.ALL)
                .build();

        PageSearchTrunk<NotificationMetadataEntity> result =
                dao.searchByIun(searchDto, PARTITION, SENT_AT.toString());

        assertNotNull(result.getResults());
        assertEquals(1, result.getResults().size());
    }

    @Test
    void searchByIunReceiverDiscardsDifferentSender() {
        mockGetItem(senderCommunicationTypeEntity("LEGAL", "PA-other-sender"));

        InputSearchNotificationDto searchDto = baseReceiverSearch()
                .filterId(SENDER_ID)
                .communicationType(NotificationSearchCommunicationType.ALL)
                .build();

        PageSearchTrunk<NotificationMetadataEntity> result =
                dao.searchByIun(searchDto, PARTITION, SENT_AT.toString());

        assertTrue(result.getResults() == null || result.getResults().isEmpty());
    }

    @Test
    void searchByIunCampaignKeepsMatchingRecipientFromFilterId() {
        NotificationMetadataEntity entity = campaignEntity();
        mockGetItem(entity);

        InputSearchNotificationDto searchDto = baseCampaignSearch()
                .filterId("recipient-external-id")
                .opaqueFilterIdPF("opaque-requested-recipient")
                .build();

        PageSearchTrunk<NotificationMetadataEntity> result =
                dao.searchByIun(searchDto, PARTITION, SENT_AT.toString());

        assertNotNull(result.getResults());
        assertEquals(1, result.getResults().size());
    }

    @Test
    void searchByIunCampaignDiscardsDifferentRecipientFromFilterId() {
        NotificationMetadataEntity entity = campaignEntity();
        entity.setRecipientIds(List.of("opaque-other-recipient"));
        mockGetItem(entity);

        InputSearchNotificationDto searchDto = baseCampaignSearch()
                .filterId("recipient-external-id")
                .opaqueFilterIdPF("opaque-requested-recipient")
                .build();

        PageSearchTrunk<NotificationMetadataEntity> result =
                dao.searchByIun(searchDto, PARTITION, SENT_AT.toString());

        assertTrue(result.getResults() == null || result.getResults().isEmpty());
    }

    @Test
    void searchByIunCampaignKeepsEntityInsideRequestedGroups() {
        NotificationMetadataEntity entity = campaignEntity();
        mockGetItem(entity);

        InputSearchNotificationDto searchDto = baseCampaignSearch()
                .groups(List.of("group-allowed"))
                .build();

        PageSearchTrunk<NotificationMetadataEntity> result =
                dao.searchByIun(searchDto, PARTITION, SENT_AT.toString());

        assertNotNull(result.getResults());
        assertEquals(1, result.getResults().size());
    }

    @Test
    void searchByIunCampaignDiscardsEntityOutsideRequestedGroups() {
        NotificationMetadataEntity entity = campaignEntity();
        entity.setNotificationGroup("group-not-allowed");
        mockGetItem(entity);

        InputSearchNotificationDto searchDto = baseCampaignSearch()
                .groups(List.of("group-allowed"))
                .build();

        PageSearchTrunk<NotificationMetadataEntity> result =
                dao.searchByIun(searchDto, PARTITION, SENT_AT.toString());

        assertTrue(result.getResults() == null || result.getResults().isEmpty());
    }

    private void assertResult(NotificationSearchCommunicationType communicationType, boolean expectedKept) {
        InputSearchNotificationDto searchDto = baseReceiverSearch()
                .communicationType(communicationType)
                .build();

        PageSearchTrunk<NotificationMetadataEntity> result =
                dao.searchByIun(searchDto, PARTITION, SENT_AT.toString());

        if (expectedKept) {
            assertNotNull(result.getResults());
            assertEquals(1, result.getResults().size());
        } else {
            assertTrue(result.getResults() == null || result.getResults().isEmpty());
        }
    }

    // ---------- WI-US3.4: flusso MITTENTE (bySender = true) ----------

    @Test
    void searchForOneMonthSenderLegalAppliesLegalOrAttributeNotExistsOnSenderIndex() {
        Expression filter = captureSenderFilterExpression(NotificationSearchCommunicationType.LEGAL);

        assertNotNull(filter.expression());
        assertTrue(filter.expression().contains(NotificationMetadataEntity.FIELD_COMMUNICATION_TYPE + " = :legal"),
                "Anche per il mittente (INDEX_BY_SENDER) deve filtrare per communicationType = :legal");
        assertTrue(filter.expression().contains("attribute_not_exists(" + NotificationMetadataEntity.FIELD_COMMUNICATION_TYPE + ")"),
                "Deve includere le notifiche storiche prive del campo anche lato mittente");
        assertEquals("LEGAL", filter.expressionValues().get(":legal").s());
        assertFalse(filter.expressionValues().containsKey(":informal"));
    }

    @Test
    void searchForOneMonthSenderLegalKeepsRecipientOneClauseTogetherWithCommunicationTypeFilter() {
        // lato mittente senza filterId viene applicato anche il filtro recipientOne: i due filtri devono coesistere
        Expression filter = captureSenderFilterExpression(NotificationSearchCommunicationType.LEGAL);

        assertNotNull(filter.expression());
        assertTrue(filter.expression().contains(NotificationMetadataEntity.FIELD_RECIPIENT_ONE + " = :recipientOne"),
                "Il filtro recipientOne del mittente deve restare presente");
        assertTrue(filter.expression().contains(NotificationMetadataEntity.FIELD_COMMUNICATION_TYPE + " = :legal"),
                "Il filtro communicationType deve essere applicato in AND con recipientOne");
        assertTrue(filter.expressionValues().containsKey(":recipientOne"));
        assertTrue(filter.expressionValues().containsKey(":legal"));
    }

    @Test
    void searchByIunSenderLegalKeepsLegalEntity() {
        mockGetItem(senderCommunicationTypeEntity("LEGAL"));
        assertSenderResult(NotificationSearchCommunicationType.LEGAL, true);
    }

    @Test
    void searchByIunSenderLegalKeepsHistoricalEntityWithoutCommunicationType() {
        mockGetItem(senderCommunicationTypeEntity(null));
        assertSenderResult(NotificationSearchCommunicationType.LEGAL, true);
    }

    @Test
    void searchByIunSenderLegalDiscardsInformalEntity() {
        mockGetItem(senderCommunicationTypeEntity("INFORMAL"));
        assertSenderResult(NotificationSearchCommunicationType.LEGAL, false);
    }

    private Expression captureSenderFilterExpression(NotificationSearchCommunicationType communicationType) {
        Page<NotificationMetadataEntity> page = Page.create(Collections.emptyList());
        when(index.query(any(QueryEnhancedRequest.class))).thenReturn(() -> List.of(page).iterator());

        InputSearchNotificationDto searchDto = baseSenderSearch()
                .communicationType(communicationType)
                .build();

        dao.searchForOneMonth(searchDto, SENDER_INDEX_NAME, SENDER_PARTITION, 10, null);

        ArgumentCaptor<QueryEnhancedRequest> captor = ArgumentCaptor.forClass(QueryEnhancedRequest.class);
        verify(index).query(captor.capture());
        return captor.getValue().filterExpression();
    }

    private void assertSenderResult(NotificationSearchCommunicationType communicationType, boolean expectedKept) {
        InputSearchNotificationDto searchDto = baseSenderSearch()
                .communicationType(communicationType)
                .build();

        PageSearchTrunk<NotificationMetadataEntity> result =
                dao.searchByIun(searchDto, SENDER_PARTITION, SENT_AT.toString());

        if (expectedKept) {
            assertNotNull(result.getResults());
            assertEquals(1, result.getResults().size());
        } else {
            assertTrue(result.getResults() == null || result.getResults().isEmpty());
        }
    }

    // ---------- helpers ----------

    private void mockGetItem(NotificationMetadataEntity entity) {
        when(table.getItem(any(GetItemEnhancedRequest.class))).thenReturn(entity);
    }

    private InputSearchNotificationDto.InputSearchNotificationDtoBuilder baseReceiverSearch() {
        return new InputSearchNotificationDto().toBuilder()
                .bySender(false)
                .senderReceiverId(RECIPIENT_ID)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .statuses(List.of())
                .size(10);
    }

    private InputSearchNotificationDto.InputSearchNotificationDtoBuilder baseSenderSearch() {
        return new InputSearchNotificationDto().toBuilder()
                .bySender(true)
                .senderReceiverId(SENDER_ID)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .statuses(List.of())
                .size(10);
    }

    private InputSearchNotificationDto.InputSearchNotificationDtoBuilder baseCampaignSearch() {
        return new InputSearchNotificationDto().toBuilder()
                .byCampaign(true)
                .campaignId("campaign-id")
                .senderReceiverId(SENDER_ID)
                .communicationType(NotificationSearchCommunicationType.INFORMAL)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .statuses(List.of())
                .size(10);
    }

    private NotificationMetadataEntity communicationTypeEntity(String communicationType) {
        return NotificationMetadataEntity.builder()
                .iunRecipientId("TGWR-ZJQN-JMAR-202209-A-1##" + RECIPIENT_ID)
                .recipientId(RECIPIENT_ID)
                .recipientIds(List.of(RECIPIENT_ID))
                .notificationGroup("")
                .notificationStatus("DELIVERING")
                .sentAt(SENT_AT)
                .communicationType(communicationType)
                .tableRow(Map.of("senderDenomination", "comune", "subject", "oggetto", "paProtocolNumber", "123"))
                .build();
    }

    private NotificationMetadataEntity campaignEntity() {
        return NotificationMetadataEntity.builder()
                .iunRecipientId("TGWR-ZJQN-JMAR-202209-A-1##" + RECIPIENT_ID)
                .recipientId(RECIPIENT_ID)
                .recipientIds(List.of("opaque-requested-recipient"))
                .notificationGroup("group-allowed")
                .notificationStatus("PROCESSING")
                .sentAt(SENT_AT)
                .campaignId("campaign-id")
                .communicationType("INFORMAL")
                .tableRow(Map.of("senderDenomination", "comune", "subject", "oggetto", "paProtocolNumber", "123"))
                .build();
    }

    private NotificationMetadataEntity senderCommunicationTypeEntity(String communicationType) {
        return senderCommunicationTypeEntity(communicationType, SENDER_ID);
    }

    private NotificationMetadataEntity senderCommunicationTypeEntity(String communicationType, String senderId) {
        return NotificationMetadataEntity.builder()
                .iunRecipientId("TGWR-ZJQN-JMAR-202209-A-1##" + RECIPIENT_ID)
                .recipientId(RECIPIENT_ID)
                .recipientIds(List.of(RECIPIENT_ID))
                .senderId(senderId)
                .notificationGroup("")
                .notificationStatus("DELIVERING")
                .sentAt(SENT_AT)
                .communicationType(communicationType)
                .tableRow(Map.of("senderDenomination", "comune", "subject", "oggetto", "paProtocolNumber", "123"))
                .build();
    }
}
