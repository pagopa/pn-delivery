package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.notificationcost.PnNotificationCostServiceClientImpl;
import it.pagopa.pn.delivery.pnclient.timelineservice.PnTimelineServiceClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

public class NotificationCostServiceFactoryTest {

    @Mock
    private PnDeliveryConfigs deliveryConfigs;
    @Mock
    private PnDeliveryPushClientImpl pnDeliveryPushClient;
    @Mock
    private PnTimelineServiceClientImpl pnTimelineServiceClient;
    @Mock
    private PnNotificationCostServiceClientImpl pnNotificationCostServiceClient;
    @Mock
    private NotificationProcessCostResponseMapper notificationMapper;

    private NotificationCostServiceFactory factory;

    @BeforeEach
    void setUp() {
        deliveryConfigs = Mockito.mock(PnDeliveryConfigs.class);
        pnDeliveryPushClient = Mockito.mock(PnDeliveryPushClientImpl.class);
        pnTimelineServiceClient = Mockito.mock(PnTimelineServiceClientImpl.class);
        pnNotificationCostServiceClient = Mockito.mock(PnNotificationCostServiceClientImpl.class);
        notificationMapper = Mockito.mock(NotificationProcessCostResponseMapper.class);

        factory = new NotificationCostServiceFactory(deliveryConfigs, pnDeliveryPushClient, pnTimelineServiceClient, pnNotificationCostServiceClient, notificationMapper);
    }

    @Test
    void testGetNotificationCostServiceBySentAt_BeforeActivationDate() {
        Instant sentAt = Instant.parse("2024-01-01T00:00:00Z");
        when(deliveryConfigs.getNewCostMsActivationDate()).thenReturn(Instant.parse("2024-06-01T00:00:00Z"));

        NotificationCostService service = factory.getNotificationCostServiceBySentAt(sentAt);
        assertThat(service).isInstanceOf(DeliveryPushNotificationCostService.class);
    }

    @Test
    void testGetNotificationCostServiceBySentAt_AfterActivationDate() {
        Instant sentAt = Instant.parse("2026-01-01T00:00:00Z");
        when(deliveryConfigs.getNewCostMsActivationDate()).thenReturn(Instant.parse("2024-06-01T00:00:00Z"));

        NotificationCostService service = factory.getNotificationCostServiceBySentAt(sentAt);
        assertThat(service).isInstanceOf(NotificationCostServiceImpl.class);
    }

    @Test
    void testGetNotificationCostServiceBySentAt_NullActivationDate() {
        Instant sentAt = Instant.parse("2026-01-01T00:00:00Z");
        when(deliveryConfigs.getNewCostMsActivationDate()).thenReturn(null);

        NotificationCostService service = factory.getNotificationCostServiceBySentAt(sentAt);
        assertThat(service).isInstanceOf(DeliveryPushNotificationCostService.class);
    }
}
