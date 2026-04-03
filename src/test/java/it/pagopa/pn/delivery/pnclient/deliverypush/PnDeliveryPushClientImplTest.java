package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationCancelledException;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.NotificationProcessCostApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.TimelineAndStatusApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

import static it.pagopa.pn.delivery.svc.NotificationPriceService.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED;
import static it.pagopa.pn.delivery.svc.NotificationPriceService.ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {PnDeliveryPushClientImpl.class})
@ExtendWith(SpringExtension.class)
class PnDeliveryPushClientImplTest {
    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.NotificationProcessCostApi")
    private NotificationProcessCostApi notificationProcessCostApi;

    @Autowired
    private PnDeliveryPushClientImpl pnDeliveryPushClientImpl;

    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.TimelineAndStatusApi")
    private TimelineAndStatusApi timelineAndStatusApi;


    @Test
    void testGetNotificationProcessCost() throws RestClientException {
        NotificationProcessCostResponse notificationProcessCostResponse = new NotificationProcessCostResponse();
        when(notificationProcessCostApi.notificationProcessCost(anyString(), anyInt(),
                any(), anyBoolean(), anyInt(), anyInt())).thenReturn(notificationProcessCostResponse);
        assertSame(notificationProcessCostResponse,
                pnDeliveryPushClientImpl.getNotificationProcessCost("Iun", 1, NotificationFeePolicy.FLAT_RATE, false, 0, 22));
        verify(notificationProcessCostApi).notificationProcessCost(anyString(), anyInt(),
                any(), anyBoolean(), anyInt(), anyInt());
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

    @Test
    void testGetNotificationProcessCost2() throws RestClientException {
        NotificationProcessCostResponse notificationProcessCostResponse = new NotificationProcessCostResponse();
        when(notificationProcessCostApi.notificationProcessCost( anyString(), anyInt(),
                any(), anyBoolean(), anyInt(), anyInt())).thenReturn(notificationProcessCostResponse);
        assertSame(notificationProcessCostResponse,
                pnDeliveryPushClientImpl.getNotificationProcessCost("Iun", 1, NotificationFeePolicy.DELIVERY_MODE, false, 0, 22));
        verify(notificationProcessCostApi).notificationProcessCost(anyString(), anyInt(),
                any(), anyBoolean(), anyInt(), anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceFailureForNotificationNotAccepted() {
        PnHttpResponseException exception =  new PnHttpResponseException("errore", 404);
        it.pagopa.pn.common.rest.error.v1.dto.ProblemError problemError = new it.pagopa.pn.common.rest.error.v1.dto.ProblemError().code(ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED);
        exception.getProblem().setErrors(Collections.singletonList(problemError));

        when(notificationProcessCostApi.notificationProcessCost( anyString(), anyInt(),
                any(), anyBoolean(), anyInt(), anyInt())).thenThrow(exception);

        Executable todo = () -> pnDeliveryPushClientImpl.getNotificationProcessCost( "iun", 1, NotificationFeePolicy.DELIVERY_MODE, false, 0, 22);
        assertThrows(PnNotFoundException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceFailureForNotificationCancelled() {
        PnHttpResponseException exception =  new PnHttpResponseException("errore", 404);
        it.pagopa.pn.common.rest.error.v1.dto.ProblemError problemError = new it.pagopa.pn.common.rest.error.v1.dto.ProblemError().code(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED);
        exception.getProblem().setErrors(Collections.singletonList(problemError));

        when(notificationProcessCostApi.notificationProcessCost( anyString(), anyInt(),
                any(), anyBoolean(), anyInt(), anyInt())).thenThrow(exception);

        Executable todo = () -> pnDeliveryPushClientImpl.getNotificationProcessCost( "iun", 1, NotificationFeePolicy.DELIVERY_MODE, false, 0, 22);
        assertThrows(PnNotificationCancelledException.class, todo);
    }
}

