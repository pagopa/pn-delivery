package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import it.pagopa.pn.delivery.models.NotificationCostRequest;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class DeliveryPushNotificationCostServiceTest {

    private PnDeliveryPushClientImpl pnDeliveryPushClient;
    private DeliveryPushNotificationCostService service;
    private NotificationProcessCostResponseMapper notificationMapper;

    @BeforeEach
    void setUp() {
        pnDeliveryPushClient = Mockito.mock(PnDeliveryPushClientImpl.class);
        notificationMapper = Mockito.mock(NotificationProcessCostResponseMapper.class);
        service = new DeliveryPushNotificationCostService(pnDeliveryPushClient, notificationMapper);
    }

    @Test
    void getNotificationCost_shouldMapResponseCorrectly() {
        NotificationCostRequest request = new NotificationCostRequest("iun", 1, NotificationFeePolicy.DELIVERY_MODE, false, 0, 22, "TEST-PA-TAX-ID", "TEST-NOTICE-CODE");
        var externalResponse = Mockito.mock(NotificationProcessCostResponse.class);
        var expectedInternalResponse = Mockito.mock(NotificationProcessCostResponseInt.class);

        when(pnDeliveryPushClient.getNotificationProcessCost("iun", 1, NotificationFeePolicy.DELIVERY_MODE, false, 0, 22))
                .thenReturn(externalResponse);
        when(notificationMapper.fromExternal(externalResponse)).thenReturn(expectedInternalResponse);

        var actualResponse = service.getNotificationCost(request);
        assertEquals(expectedInternalResponse, actualResponse);
        Mockito.verify(pnDeliveryPushClient).getNotificationProcessCost("iun", 1, NotificationFeePolicy.DELIVERY_MODE, false, 0, 22);
        Mockito.verify(notificationMapper).fromExternal(externalResponse);
    }

    @Test
    void getNotificationCost_shouldPropagateClientException() {
        NotificationCostRequest request = new NotificationCostRequest("iun", 1, NotificationFeePolicy.DELIVERY_MODE, false, 0, 22, "TEST-PA-TAX-ID", "TEST-NOTICE-CODE");
        when(pnDeliveryPushClient.getNotificationProcessCost("iun", 1, NotificationFeePolicy.DELIVERY_MODE, false, 0, 22))
                .thenThrow(new RuntimeException("client error"));
        assertThrows(RuntimeException.class, () -> service.getNotificationCost(request));
    }
}
