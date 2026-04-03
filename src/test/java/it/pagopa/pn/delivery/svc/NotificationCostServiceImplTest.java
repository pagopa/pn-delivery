package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationCancelledException;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.NotificationCostRecipientResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.timelineservice.v1.model.DeliveryInformationResponse;
import it.pagopa.pn.delivery.models.NotificationCostRequest;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;
import it.pagopa.pn.delivery.pnclient.notificationcost.PnNotificationCostServiceClientImpl;
import it.pagopa.pn.delivery.pnclient.timelineservice.PnTimelineServiceClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class NotificationCostServiceImplTest {

    private PnTimelineServiceClientImpl pnTimelineServiceClient;
    private PnNotificationCostServiceClientImpl pnNotificationCostServiceClient;
    private NotificationProcessCostResponseMapper notificationMapper;
    private NotificationCostServiceImpl service;

    @BeforeEach
    void setUp() {
        pnTimelineServiceClient = Mockito.mock(PnTimelineServiceClientImpl.class);
        pnNotificationCostServiceClient = Mockito.mock(PnNotificationCostServiceClientImpl.class);
        notificationMapper = Mockito.mock(NotificationProcessCostResponseMapper.class);
        service = new NotificationCostServiceImpl(pnTimelineServiceClient, pnNotificationCostServiceClient, notificationMapper);
    }

    @Test
    void getNotificationCost_shouldThrowIfCancelled() {
        NotificationCostRequest request = new NotificationCostRequest("iun", 1, null, false, 0, 22);
        DeliveryInformationResponse deliveryInfo = Mockito.mock(DeliveryInformationResponse.class);
        when(pnTimelineServiceClient.getDeliveryInformation("iun", 1)).thenReturn(deliveryInfo);
        when(deliveryInfo.getIsNotificationCancelled()).thenReturn(true);
        assertThrows(PnNotificationCancelledException.class, () -> service.getNotificationCost(request));
    }

    @Test
    void getNotificationCost_shouldThrowIfNotAccepted() {
        NotificationCostRequest request = new NotificationCostRequest("iun", 1, null, false, 0, 22);
        DeliveryInformationResponse deliveryInfo = Mockito.mock(DeliveryInformationResponse.class);
        when(pnTimelineServiceClient.getDeliveryInformation("iun", 1)).thenReturn(deliveryInfo);
        when(deliveryInfo.getIsNotificationCancelled()).thenReturn(false);
        when(deliveryInfo.getIsNotificationAccepted()).thenReturn(false);
        assertThrows(PnNotFoundException.class, () -> service.getNotificationCost(request));
    }

    @Test
    void getNotificationCost_shouldMapResponseCorrectly() {
        NotificationCostRequest request = new NotificationCostRequest("iun", 1, null, false, 0, 22);
        DeliveryInformationResponse deliveryInfo = Mockito.mock(DeliveryInformationResponse.class);
        NotificationCostRecipientResponse externalResponse = Mockito.mock(NotificationCostRecipientResponse.class);
        NotificationProcessCostResponseInt expectedInternalResponse = Mockito.mock(NotificationProcessCostResponseInt.class);

        Mockito.when(pnTimelineServiceClient.getDeliveryInformation("iun", 1)).thenReturn(deliveryInfo);
        Mockito.when(deliveryInfo.getIsNotificationCancelled()).thenReturn(false);
        Mockito.when(deliveryInfo.getIsNotificationAccepted()).thenReturn(true);
        Mockito.when(pnNotificationCostServiceClient.getNotificationCostRecipient("iun", 1)).thenReturn(externalResponse);
        Mockito.when(notificationMapper.mapFromTimelineAndCostResponse(deliveryInfo, externalResponse)).thenReturn(expectedInternalResponse);

        NotificationProcessCostResponseInt actualResponse = service.getNotificationCost(request);
        assertEquals(expectedInternalResponse, actualResponse);
        Mockito.verify(pnTimelineServiceClient).getDeliveryInformation("iun", 1);
        Mockito.verify(pnNotificationCostServiceClient).getNotificationCostRecipient("iun", 1);
        Mockito.verify(notificationMapper).mapFromTimelineAndCostResponse(deliveryInfo, externalResponse);
    }
}
