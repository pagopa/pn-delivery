package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.NotificationCostRequest;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;
import it.pagopa.pn.delivery.utils.FeatureFlagUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCostServiceMonitorTest {

    @Mock
    private FeatureFlagUtils featureFlagUtils;
    @Mock
    private NotificationCostServiceImpl notificationCostService;

    private NotificationCostServiceMonitor serviceMonitor;

    @BeforeEach
    void setUp() {
        serviceMonitor = new NotificationCostServiceMonitor(featureFlagUtils, notificationCostService);
    }

    @Test
    void monitorNewNotificationPriceService_doesNotCallNewService_whenMonitoringIsDisabled() {
        InternalNotification notification = buildNotification();
        NotificationCostRequest costRequest = buildCostRequest();
        NotificationProcessCostResponseInt legacyResponse = buildResponse(1250, 1563, 980, 200, 70, 22);
        Instant sentAtInstant = notification.getSentAt().toInstant();

        when(featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAtInstant)).thenReturn(false);

        serviceMonitor.monitorNewNotificationPriceService(notification, costRequest, legacyResponse);

        verify(featureFlagUtils).isMonitoringOfNewCostServiceEnabled(sentAtInstant);
        verifyNoInteractions(notificationCostService);
    }

    @Test
    void monitorNewNotificationPriceService_callsNewService_whenMonitoringIsEnabled() {
        InternalNotification notification = buildNotification();
        NotificationCostRequest costRequest = buildCostRequest();
        NotificationProcessCostResponseInt legacyResponse = buildResponse(1250, 1563, 980, 200, 70, 22);
        NotificationProcessCostResponseInt newServiceResponse = buildResponse(1250, 1563, 980, 200, 70, 22);
        Instant sentAtInstant = notification.getSentAt().toInstant();

        when(featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAtInstant)).thenReturn(true);
        when(notificationCostService.getNotificationCostForMonitoring(costRequest)).thenReturn(newServiceResponse);

        serviceMonitor.monitorNewNotificationPriceService(notification, costRequest, legacyResponse);

        verify(featureFlagUtils).isMonitoringOfNewCostServiceEnabled(sentAtInstant);
        verify(notificationCostService).getNotificationCostForMonitoring(costRequest);
    }

    @Test
    void monitorNewNotificationPriceService_doesNotThrow_whenLegacyResponseIsNull() {
        InternalNotification notification = buildNotification();
        NotificationCostRequest costRequest = buildCostRequest();
        NotificationProcessCostResponseInt newServiceResponse = buildResponse(1250, 1563, 980, 200, 70, 22);
        Instant sentAtInstant = notification.getSentAt().toInstant();

        when(featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAtInstant)).thenReturn(true);
        when(notificationCostService.getNotificationCostForMonitoring(costRequest)).thenReturn(newServiceResponse);

        assertDoesNotThrow(() -> serviceMonitor.monitorNewNotificationPriceService(notification, costRequest, null));
        verify(notificationCostService).getNotificationCostForMonitoring(costRequest);
    }

    @Test
    void monitorNewNotificationPriceService_doesNotThrow_whenNewServiceResponseIsNull() {
        InternalNotification notification = buildNotification();
        NotificationCostRequest costRequest = buildCostRequest();
        NotificationProcessCostResponseInt legacyResponse = buildResponse(1250, 1563, 980, 200, 70, 22);
        Instant sentAtInstant = notification.getSentAt().toInstant();

        when(featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAtInstant)).thenReturn(true);
        when(notificationCostService.getNotificationCostForMonitoring(costRequest)).thenReturn(null);

        assertDoesNotThrow(() -> serviceMonitor.monitorNewNotificationPriceService(notification, costRequest, legacyResponse));
        verify(notificationCostService).getNotificationCostForMonitoring(costRequest);
    }

    @Test
    void monitorNewNotificationPriceService_doesNotPropagateException_whenNewServiceFails() {
        InternalNotification notification = buildNotification();
        NotificationCostRequest costRequest = buildCostRequest();
        NotificationProcessCostResponseInt legacyResponse = buildResponse(1250, 1563, 980, 200, 70, 22);
        Instant sentAtInstant = notification.getSentAt().toInstant();

        when(featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAtInstant)).thenReturn(true);
        when(notificationCostService.getNotificationCostForMonitoring(costRequest)).thenThrow(new RuntimeException("service failure"));

        assertDoesNotThrow(() -> serviceMonitor.monitorNewNotificationPriceService(notification, costRequest, legacyResponse));
        verify(notificationCostService).getNotificationCostForMonitoring(costRequest);
    }

    private InternalNotification buildNotification() {
        return InternalNotification.builder()
                .iun("IUN-123")
                .sentAt(OffsetDateTime.parse("2026-04-14T10:00:00Z"))
                .build();
    }

    private NotificationCostRequest buildCostRequest() {
        return new NotificationCostRequest("IUN-123", 0, null, true, 200, 22, "PA_TAX_ID", "NOTICE_CODE");
    }

    private NotificationProcessCostResponseInt buildResponse(Integer partialCost, Integer totalCost, Integer analogCost,
                                                             Integer paFee, Integer sendFee, Integer vat) {
        NotificationProcessCostResponseInt response = new NotificationProcessCostResponseInt();
        response.setPartialCost(partialCost);
        response.setTotalCost(totalCost);
        response.setAnalogCost(analogCost);
        response.setPaFee(paFee);
        response.setSendFee(sendFee);
        response.setVat(vat);
        return response;
    }
}