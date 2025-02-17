package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRefusedVerificationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRefusedVerificationDaoDynamoTest {

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private DynamoDbTable<NotificationRefusedVerificationEntity> dynamoDbTable;

    @Mock
    private PnDeliveryConfigs pnDeliveryConfigs;

    @Mock
    private PnDeliveryConfigs.NotificationRefusedVerificationDao notificationRefusedVerificationDao;

    private NotificationRefusedVerificationDaoDynamo notificationRefusedVerificationDaoDynamo;

    @BeforeEach
    void setUp() {
        when(pnDeliveryConfigs.getNotificationRefusedVerificationDao()).thenReturn(notificationRefusedVerificationDao);
        when(notificationRefusedVerificationDao.getTableName()).thenReturn("testTableName");
        when(dynamoDbEnhancedClient.table(anyString(), any())).then(in -> dynamoDbTable);
        notificationRefusedVerificationDaoDynamo = new NotificationRefusedVerificationDaoDynamo(dynamoDbEnhancedClient, pnDeliveryConfigs);
    }

    @Test
    void putNotificationRefusedVerification_success() {
        String pk = "testPk";
        doNothing().when(dynamoDbTable).putItem(any(PutItemEnhancedRequest.class));

        boolean result = notificationRefusedVerificationDaoDynamo.putNotificationRefusedVerification(pk);

        assertTrue(result);
    }

    @Test
    void putNotificationRefusedVerification_conditionalCheckFailed() {
        String pk = "testPk";
        doThrow(ConditionalCheckFailedException.class).when(dynamoDbTable).putItem(any(PutItemEnhancedRequest.class));

        boolean result = notificationRefusedVerificationDaoDynamo.putNotificationRefusedVerification(pk);

        assertFalse(result);
    }

    @Test
    void putNotificationRefusedVerification_dynamoDbException() {
        String pk = "testPk";
        doThrow(DynamoDbException.class).when(dynamoDbTable).putItem(any(PutItemEnhancedRequest.class));

        assertThrows(PnInternalException.class, () -> notificationRefusedVerificationDaoDynamo.putNotificationRefusedVerification(pk));
    }
}