package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.OffsetDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaNotificationLimitDaoDynamoTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private PnDeliveryConfigs pnDeliveryConfigs;

    @Mock
    private PnDeliveryConfigs.PaNotificationLimitDao paNotificationLimitDao;

    private PaNotificationLimitDaoDynamo paNotificationLimitDaoDynamo;
    private String paId;
    private OffsetDateTime sentAt;

    @BeforeEach
    void setUp() {
        Mockito.when(pnDeliveryConfigs.getPaNotificationLimitDao()).thenReturn(paNotificationLimitDao);
        paNotificationLimitDaoDynamo = new PaNotificationLimitDaoDynamo(dynamoDbClient, pnDeliveryConfigs);
        paId = "testPaId";
        sentAt = OffsetDateTime.now();
    }

    @Test
    void decrementLimitIncrementDailyCounter_success() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(UpdateItemResponse.builder().build());

        boolean result = paNotificationLimitDaoDynamo.decrementLimitIncrementDailyCounter(paId, sentAt);

        assertTrue(result);
        verify(dynamoDbClient).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void decrementLimitIncrementDailyCounter_conditionalCheckFailed() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenThrow(ConditionalCheckFailedException.builder().build());

        boolean result = paNotificationLimitDaoDynamo.decrementLimitIncrementDailyCounter(paId, sentAt);

        assertFalse(result);
        verify(dynamoDbClient).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void incrementLimitDecrementDailyCounter_success() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(UpdateItemResponse.builder().build());

        paNotificationLimitDaoDynamo.incrementLimitDecrementDailyCounter(paId, sentAt);

        verify(dynamoDbClient).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void incrementLimitDecrementDailyCounter_conditionalCheckFailed() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenThrow(ConditionalCheckFailedException.builder().build());

        paNotificationLimitDaoDynamo.incrementLimitDecrementDailyCounter(paId, sentAt);

        verify(dynamoDbClient).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void checkIfPaNotificationLimitExists_itemExists() {
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(new HashMap<>()).build());

        boolean result = paNotificationLimitDaoDynamo.checkIfPaNotificationLimitExists(paId, sentAt);

        assertTrue(result);
        verify(dynamoDbClient).getItem(any(GetItemRequest.class));
    }

    @Test
    void checkIfPaNotificationLimitExists_itemDoesNotExist() {
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        boolean result = paNotificationLimitDaoDynamo.checkIfPaNotificationLimitExists(paId, sentAt);

        assertFalse(result);
        verify(dynamoDbClient).getItem(any(GetItemRequest.class));
    }
}