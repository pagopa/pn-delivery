package it.pagopa.pn.delivery.pnclient.notificationcost;

import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.api.NotificationCostRecipientApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.NotificationCostRecipientResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PnNotificationCostServiceClientImplTest {

    @Mock
    private NotificationCostRecipientApi notificationCostRecipientApi;

    @InjectMocks
    private PnNotificationCostServiceClientImpl pnNotificationCostServiceClientImpl;

    @Test
    void getNotificationCostRecipient() {
        String iun = "test_iun";
        Integer recIndex = 1;

        NotificationCostRecipientResponse mockResponse = mock(NotificationCostRecipientResponse.class);
        when(notificationCostRecipientApi.getNotificationCost(iun, recIndex)).thenReturn(mockResponse);

        NotificationCostRecipientResponse response = pnNotificationCostServiceClientImpl.getNotificationCostRecipient(iun, recIndex);

        assertNotNull(response);
        assertEquals(mockResponse, response);
        verify(notificationCostRecipientApi, times(1)).getNotificationCost(iun, recIndex);
    }
}
