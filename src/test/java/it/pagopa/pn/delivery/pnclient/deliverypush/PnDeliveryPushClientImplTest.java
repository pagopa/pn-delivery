package it.pagopa.pn.delivery.pnclient.deliverypush;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.NotificationProcessCostApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.TimelineAndStatusApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;

@ContextConfiguration(classes = {PnDeliveryPushClientImpl.class})
@ExtendWith(SpringExtension.class)
class PnDeliveryPushClientImplTest {
    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.NotificationProcessCostApi")
    private NotificationProcessCostApi notificationProcessCostApi;

    @Autowired
    private PnDeliveryPushClientImpl pnDeliveryPushClientImpl;

    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.TimelineAndStatusApi")
    private TimelineAndStatusApi timelineAndStatusApi;

    /**
     * Method under test: {@link PnDeliveryPushClientImpl#getNotificationProcessCost(String, int, NotificationFeePolicy)}
     */
    @Test
    void testGetNotificationProcessCost() throws RestClientException {
        NotificationProcessCostResponse notificationProcessCostResponse = new NotificationProcessCostResponse();
        when(notificationProcessCostApi.notificationProcessCost(Mockito.<String>any(), Mockito.<Integer>any(),
                Mockito.<NotificationFeePolicy>any())).thenReturn(notificationProcessCostResponse);
        assertSame(notificationProcessCostResponse,
                pnDeliveryPushClientImpl.getNotificationProcessCost("Iun", 1, NotificationFeePolicy.FLAT_RATE));
        verify(notificationProcessCostApi).notificationProcessCost(Mockito.<String>any(), Mockito.<Integer>any(),
                Mockito.<NotificationFeePolicy>any());
    }

    /**
     * Method under test: {@link PnDeliveryPushClientImpl#getTimelineAndStatusHistory(String, int, OffsetDateTime)}
     */
    @Test
    void testGetTimelineAndStatusHistory() throws RestClientException {
        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        when(timelineAndStatusApi.getNotificationHistory(Mockito.<String>any(), Mockito.<Integer>any(),
                Mockito.<OffsetDateTime>any())).thenReturn(notificationHistoryResponse);
        assertSame(notificationHistoryResponse, pnDeliveryPushClientImpl.getTimelineAndStatusHistory("Iun", 10,
                OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC)));
        verify(timelineAndStatusApi).getNotificationHistory(Mockito.<String>any(), Mockito.<Integer>any(),
                Mockito.<OffsetDateTime>any());
    }

    /**
     * Method under test: {@link PnDeliveryPushClientImpl#getNotificationProcessCost(String, int, NotificationFeePolicy)}
     */
    @Test
    void testGetNotificationProcessCost2() throws RestClientException {
        NotificationProcessCostResponse notificationProcessCostResponse = new NotificationProcessCostResponse();
        when(notificationProcessCostApi.notificationProcessCost(Mockito.<String>any(), Mockito.<Integer>any(),
                Mockito.<NotificationFeePolicy>any())).thenReturn(notificationProcessCostResponse);
        assertSame(notificationProcessCostResponse,
                pnDeliveryPushClientImpl.getNotificationProcessCost("Iun", 1, NotificationFeePolicy.DELIVERY_MODE));
        verify(notificationProcessCostApi).notificationProcessCost(Mockito.<String>any(), Mockito.<Integer>any(),
                Mockito.<NotificationFeePolicy>any());
    }
}

