package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.utils.FeatureFlagUtils;
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
    private DeliveryPushNotificationCostService deliveryPushNotificationCostService;
    @Mock
    private NotificationCostServiceImpl notificationCostService;
    @Mock
    private FeatureFlagUtils featureFlagUtils;

    private NotificationCostServiceFactory factory;

    @BeforeEach
    void setUp() {
        featureFlagUtils = Mockito.mock(FeatureFlagUtils.class);
        deliveryPushNotificationCostService = Mockito.mock(DeliveryPushNotificationCostService.class);
        notificationCostService = Mockito.mock(NotificationCostServiceImpl.class);


        factory = new NotificationCostServiceFactory(deliveryPushNotificationCostService, notificationCostService, featureFlagUtils);
    }

    @Test
    void testGetNotificationCostServiceBySentAt_WhenIntegrationIsDisabled() {
        Instant sentAt = Instant.parse("2024-01-01T00:00:00Z");
        when(featureFlagUtils.isIntegrationWithNewCostServiceEnabled(sentAt)).thenReturn(false);

        NotificationCostService service = factory.getNotificationCostServiceBySentAt(sentAt);
        assertThat(service).isInstanceOf(DeliveryPushNotificationCostService.class);
    }

    @Test
    void testGetNotificationCostServiceBySentAt_WhenIntegrationIsEnabled() {
        Instant sentAt = Instant.parse("2026-01-01T00:00:00Z");
        when(featureFlagUtils.isIntegrationWithNewCostServiceEnabled(sentAt)).thenReturn(true);

        NotificationCostService service = factory.getNotificationCostServiceBySentAt(sentAt);
        assertThat(service).isInstanceOf(NotificationCostServiceImpl.class);
    }

    @Test
    void getNotificationCostServiceBySentAt_shouldThrowException_whenSentAtIsNull() {
        assertThrows(PnInternalException.class, () -> factory.getNotificationCostServiceBySentAt(null)
        );
    }
}
