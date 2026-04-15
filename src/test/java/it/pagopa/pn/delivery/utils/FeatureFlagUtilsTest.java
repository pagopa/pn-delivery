package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeatureFlagUtilsTest {
    private PnDeliveryConfigs pnDeliveryConfigs;
    private FeatureFlagUtils featureFlagUtils;

    @BeforeEach
    void setUp() {
        pnDeliveryConfigs = mock(PnDeliveryConfigs.class);
        featureFlagUtils = new FeatureFlagUtils(pnDeliveryConfigs);
    }

    @Test
    void isPhysicalAddressLookupEnabled_returnsFalse_whenStartDateIsNull() {
        when(pnDeliveryConfigs.getPhysicalAddressLookupStartDate()).thenReturn(null);

        boolean result = featureFlagUtils.isPhysicalAddressLookupEnabled();

        assertFalse(result);
    }

    @Test
    void isPhysicalAddressLookupEnabled_returnsFalse_whenStartDateIsInFuture() {
        Instant futureDate = Instant.now().plusSeconds(3600);
        when(pnDeliveryConfigs.getPhysicalAddressLookupStartDate()).thenReturn(futureDate);

        boolean result = featureFlagUtils.isPhysicalAddressLookupEnabled();

        assertFalse(result);
    }

    @Test
    void isPhysicalAddressLookupEnabled_returnsTrue_whenStartDateIsInPast() {
        Instant pastDate = Instant.now().minusSeconds(3600);
        when(pnDeliveryConfigs.getPhysicalAddressLookupStartDate()).thenReturn(pastDate);

        boolean result = featureFlagUtils.isPhysicalAddressLookupEnabled();

        assertTrue(result);
    }

    @Test
    void isPhysicalAddressLookupEnabled_returnsTrue_whenStartDateIsNow() {
        Instant now = Instant.now();
        when(pnDeliveryConfigs.getPhysicalAddressLookupStartDate()).thenReturn(now);

        boolean result = featureFlagUtils.isPhysicalAddressLookupEnabled();

        assertTrue(result);
    }

    @Test
    void isIntegrationWithNewCostServiceEnabled_returnsFalse_whenStartDateIsNull() {
        Instant sentAt = Instant.parse("2026-04-14T10:00:00Z");
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(null);

        boolean result = featureFlagUtils.isIntegrationWithNewCostServiceEnabled(sentAt);

        assertFalse(result);
    }

    @Test
    void isIntegrationWithNewCostServiceEnabled_returnsTrue_whenStartDateIsEqualToSentAt() {
        Instant sentAt = Instant.parse("2026-04-14T10:00:00Z");
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(sentAt);

        boolean result = featureFlagUtils.isIntegrationWithNewCostServiceEnabled(sentAt);

        assertTrue(result);
    }

    @Test
    void isIntegrationWithNewCostServiceEnabled_returnsTrue_whenStartDateIsBeforeSentAt() {
        Instant sentAt = Instant.parse("2026-04-14T10:00:00Z");
        Instant startDate = sentAt.minusSeconds(60);
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(startDate);

        boolean result = featureFlagUtils.isIntegrationWithNewCostServiceEnabled(sentAt);

        assertTrue(result);
    }

    @Test
    void isIntegrationWithNewCostServiceEnabled_returnsFalse_whenStartDateIsAfterSentAt() {
        Instant sentAt = Instant.parse("2026-04-14T10:00:00Z");
        Instant startDate = sentAt.plusSeconds(60);
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(startDate);

        boolean result = featureFlagUtils.isIntegrationWithNewCostServiceEnabled(sentAt);

        assertFalse(result);
    }

    @Test
    void isIntegrationWithNewCostServiceEnabled_throwsException_whenSentAtIsNull() {
        Instant startDate = Instant.parse("2026-04-14T10:00:00Z");
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(startDate);

        assertThrows(NullPointerException.class, () -> featureFlagUtils.isIntegrationWithNewCostServiceEnabled(null));
    }

    @Test
    void isMonitoringOfNewCostServiceEnabled_returnsFalse_whenIntegrationIsEnabled() {
        Instant sentAt = Instant.parse("2026-04-14T10:00:00Z");
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(sentAt.minusSeconds(60));

        boolean result = featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAt);

        assertFalse(result);
        verify(pnDeliveryConfigs, never()).isNewCostServiceMonitoringEnabled();
        verify(pnDeliveryConfigs, never()).getNewCostServiceNotificationProcessingStartDate();
    }

    @Test
    void isMonitoringOfNewCostServiceEnabled_returnsTrue_whenIntegrationDisabled_flagEnabled_andStartDateBeforeSentAt() {
        Instant sentAt = Instant.parse("2026-04-14T10:00:00Z");
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(sentAt.plusSeconds(60));
        when(pnDeliveryConfigs.isNewCostServiceMonitoringEnabled()).thenReturn(true);
        when(pnDeliveryConfigs.getNewCostServiceNotificationProcessingStartDate()).thenReturn(sentAt.minusSeconds(60));

        boolean result = featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAt);

        assertTrue(result);
    }

    @Test
    void isMonitoringOfNewCostServiceEnabled_returnsTrue_whenIntegrationDisabled_flagEnabled_andStartDateEqualsSentAt() {
        Instant sentAt = Instant.parse("2026-04-14T10:00:00Z");
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(sentAt.plusSeconds(60));
        when(pnDeliveryConfigs.isNewCostServiceMonitoringEnabled()).thenReturn(true);
        when(pnDeliveryConfigs.getNewCostServiceNotificationProcessingStartDate()).thenReturn(sentAt);

        boolean result = featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAt);

        assertTrue(result);
    }

    @Test
    void isMonitoringOfNewCostServiceEnabled_returnsFalse_whenIntegrationDisabled_butFlagDisabled() {
        Instant sentAt = Instant.parse("2026-04-14T10:00:00Z");
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(sentAt.plusSeconds(60));
        when(pnDeliveryConfigs.isNewCostServiceMonitoringEnabled()).thenReturn(false);
        when(pnDeliveryConfigs.getNewCostServiceNotificationProcessingStartDate()).thenReturn(sentAt.minusSeconds(60));

        boolean result = featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAt);

        assertFalse(result);
    }

    @Test
    void isMonitoringOfNewCostServiceEnabled_returnsFalse_whenIntegrationDisabled_flagEnabled_butNewCostServiceNotificationProcessingStartDateAfterSentAt() {
        Instant sentAt = Instant.parse("2026-04-14T10:00:00Z");
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(sentAt.plusSeconds(60));
        when(pnDeliveryConfigs.isNewCostServiceMonitoringEnabled()).thenReturn(true);
        when(pnDeliveryConfigs.getNewCostServiceNotificationProcessingStartDate()).thenReturn(sentAt.plusSeconds(60));

        boolean result = featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAt);

        assertFalse(result);
    }

    @Test
    void isMonitoringOfNewCostServiceEnabled_returnsFalse_whenNewCostServiceNotificationProcessingStartDateIsNull_flagEnabled_andIntegrationDisabled() {
        Instant sentAt = Instant.parse("2026-04-14T10:00:00Z");
        when(pnDeliveryConfigs.getNewCostServiceActivationDate()).thenReturn(sentAt.plusSeconds(60));
        when(pnDeliveryConfigs.isNewCostServiceMonitoringEnabled()).thenReturn(true);
        when(pnDeliveryConfigs.getNewCostServiceNotificationProcessingStartDate()).thenReturn(null);

        boolean result = featureFlagUtils.isMonitoringOfNewCostServiceEnabled(sentAt);

        assertFalse(result);
    }

}