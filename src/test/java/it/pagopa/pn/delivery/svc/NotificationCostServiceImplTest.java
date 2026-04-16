package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationCancelledException;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.NotificationCostPaymentResponse;
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
        NotificationCostRequest request = new NotificationCostRequest("iun", 1, null, false, 0, 22, "TEST-TAX-ID", "TEST-NOTICE-CODE");
        DeliveryInformationResponse deliveryInfo = Mockito.mock(DeliveryInformationResponse.class);
        when(pnTimelineServiceClient.getDeliveryInformation("iun", 1)).thenReturn(deliveryInfo);
        when(deliveryInfo.getIsNotificationCancelled()).thenReturn(true);
        assertThrows(PnNotificationCancelledException.class, () -> service.getNotificationCost(request));
    }

    @Test
    void getNotificationCost_shouldThrowIfNotAccepted() {
        NotificationCostRequest request = new NotificationCostRequest("iun", 1, null, false, 0, 22, "TEST-TAX-ID", "TEST-NOTICE-CODE");
        DeliveryInformationResponse deliveryInfo = Mockito.mock(DeliveryInformationResponse.class);
        when(pnTimelineServiceClient.getDeliveryInformation("iun", 1)).thenReturn(deliveryInfo);
        when(deliveryInfo.getIsNotificationCancelled()).thenReturn(false);
        when(deliveryInfo.getIsNotificationAccepted()).thenReturn(false);
        assertThrows(PnNotFoundException.class, () -> service.getNotificationCost(request));
    }

    @Test
    void getNotificationCost_shouldMapResponseCorrectly() {
        NotificationCostRequest request = new NotificationCostRequest("iun", 1, null, false, 0, 22,"TEST-TAX-ID", "TEST-NOTICE-CODE");
        DeliveryInformationResponse deliveryInfo = Mockito.mock(DeliveryInformationResponse.class);
        NotificationCostPaymentResponse externalResponse = Mockito.mock(NotificationCostPaymentResponse.class);
        NotificationProcessCostResponseInt expectedInternalResponse = Mockito.mock(NotificationProcessCostResponseInt.class);

        Mockito.when(pnTimelineServiceClient.getDeliveryInformation("iun", 1)).thenReturn(deliveryInfo);
        Mockito.when(deliveryInfo.getIsNotificationCancelled()).thenReturn(false);
        Mockito.when(deliveryInfo.getIsNotificationAccepted()).thenReturn(true);
        Mockito.when(pnNotificationCostServiceClient.getNotificationCostByPayment("TEST-TAX-ID", "TEST-NOTICE-CODE")).thenReturn(externalResponse);
        Mockito.when(notificationMapper.mapFromTimelineAndCostResponse(deliveryInfo, externalResponse, 22)).thenReturn(expectedInternalResponse);

        NotificationProcessCostResponseInt actualResponse = service.getNotificationCost(request);
        assertEquals(expectedInternalResponse, actualResponse);
        Mockito.verify(pnTimelineServiceClient).getDeliveryInformation("iun", 1);
        Mockito.verify(pnNotificationCostServiceClient).getNotificationCostByPayment("TEST-TAX-ID", "TEST-NOTICE-CODE");
        Mockito.verify(notificationMapper).mapFromTimelineAndCostResponse(deliveryInfo, externalResponse, 22);
    }

    @Test
    void getNotificationCostForMonitoring_shouldMapResponseCorrectly() {
        NotificationCostRequest request = new NotificationCostRequest("iun", 1, null, false, 0, 22,"TEST-TAX-ID", "TEST-NOTICE-CODE");
        NotificationCostPaymentResponse externalResponse = Mockito.mock(NotificationCostPaymentResponse.class);
        NotificationProcessCostResponseInt expectedInternalResponse = Mockito.mock(NotificationProcessCostResponseInt.class);

        Mockito.when(pnNotificationCostServiceClient.getNotificationCostByPaymentForMonitoring("TEST-TAX-ID", "TEST-NOTICE-CODE")).thenReturn(externalResponse);
        Mockito.when(notificationMapper.mapFromTimelineAndCostResponse(Mockito.any(), Mockito.eq(externalResponse), Mockito.eq(22))).thenReturn(expectedInternalResponse);

        NotificationProcessCostResponseInt actualResponse = service.getNotificationCostForMonitoring(request);
        assertEquals(expectedInternalResponse, actualResponse);
        Mockito.verify(pnTimelineServiceClient, Mockito.never()).getDeliveryInformation(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(pnNotificationCostServiceClient).getNotificationCostByPaymentForMonitoring("TEST-TAX-ID", "TEST-NOTICE-CODE");
        Mockito.verify(pnNotificationCostServiceClient, Mockito.never()).getNotificationCostByPayment(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(notificationMapper).mapFromTimelineAndCostResponse(Mockito.any(), Mockito.eq(externalResponse), Mockito.eq(22));
    }
}
