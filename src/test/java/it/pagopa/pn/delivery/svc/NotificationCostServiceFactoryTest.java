package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class NotificationCostServiceFactoryTest {

    @Mock
    private PnDeliveryConfigs deliveryConfigs;
    @Mock
    private DeliveryPushNotificationCostService deliveryPushNotificationCostService;
    @Mock
    private NotificationCostServiceImpl notificationCostService;

    private NotificationCostServiceFactory factory;

    @BeforeEach
    void setUp() {
        deliveryConfigs = Mockito.mock(PnDeliveryConfigs.class);
        deliveryPushNotificationCostService = Mockito.mock(DeliveryPushNotificationCostService.class);
        notificationCostService = Mockito.mock(NotificationCostServiceImpl.class);

        factory = new NotificationCostServiceFactory(deliveryConfigs, deliveryPushNotificationCostService, notificationCostService);
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

    @Test
    void getNotificationCostServiceBySentAt_shouldThrowException_whenSentAtIsNull() {
        assertThrows(PnInternalException.class, () -> factory.getNotificationCostServiceBySentAt(null)
        );
    }
}
