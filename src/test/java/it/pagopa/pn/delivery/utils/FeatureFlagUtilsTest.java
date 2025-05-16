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
}