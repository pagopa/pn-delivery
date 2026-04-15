package it.pagopa.pn.delivery.pnclient.notificationcost;

import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.api.NotificationCostRecipientApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.NotificationCostPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PnNotificationCostServiceClientImplTest {

    private NotificationCostRecipientApi notificationCostRecipientApi;
    private NotificationCostRecipientApi monitoringNotificationCostRecipientApi;
    private PnNotificationCostServiceClientImpl pnNotificationCostServiceClientImpl;


    @BeforeEach
    void setup() {
        monitoringNotificationCostRecipientApi = mock(NotificationCostRecipientApi.class);
        notificationCostRecipientApi = mock(NotificationCostRecipientApi.class);
        pnNotificationCostServiceClientImpl = new PnNotificationCostServiceClientImpl(notificationCostRecipientApi, monitoringNotificationCostRecipientApi);
    }

    @Test
    void getNotificationCostRecipient() {
        String paTaxId = "test_pa_tax_id";
        String noticeCode = "test_notice_code";

        NotificationCostPaymentResponse mockResponse = new NotificationCostPaymentResponse();
        when(notificationCostRecipientApi.getNotificationCostByPayment(paTaxId, noticeCode)).thenReturn(mockResponse);

        NotificationCostPaymentResponse response = pnNotificationCostServiceClientImpl.getNotificationCostByPayment(paTaxId, noticeCode);

        assertNotNull(response);
        assertEquals(mockResponse, response);
        verify(notificationCostRecipientApi, times(1)).getNotificationCostByPayment(paTaxId, noticeCode);
        verify(monitoringNotificationCostRecipientApi, times(0)).getNotificationCostByPayment(anyString(), anyString());
    }

    @Test
    void getNotificationCostByPaymentForMonitoring() {
        String paTaxId = "test_pa_tax_id";
        String noticeCode = "test_notice_code";

        NotificationCostPaymentResponse mockResponse = new NotificationCostPaymentResponse();
        when(monitoringNotificationCostRecipientApi.getNotificationCostByPayment(paTaxId, noticeCode)).thenReturn(mockResponse);

        NotificationCostPaymentResponse response = pnNotificationCostServiceClientImpl.getNotificationCostByPaymentForMonitoring(paTaxId, noticeCode);

        assertNotNull(response);
        assertEquals(mockResponse, response);
        verify(monitoringNotificationCostRecipientApi, times(1)).getNotificationCostByPayment(paTaxId, noticeCode);
        verify(notificationCostRecipientApi, times(0)).getNotificationCostByPayment(anyString(), anyString());
    }
}
