package it.pagopa.pn.delivery.svc.util;

import it.pagopa.pn.delivery.config.PhysicalAddressLookupParameterConsumer;
import it.pagopa.pn.delivery.utils.FeatureFlagUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PhysicalAddressLookupUtilTest {

    @Mock
    private PhysicalAddressLookupParameterConsumer physicalAddressLookupParameter;

    @Mock
    private FeatureFlagUtils featureFlagUtils;

    private PhysicalAddressLookupUtil physicalAddressLookupUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        physicalAddressLookupUtil = new PhysicalAddressLookupUtil(physicalAddressLookupParameter, featureFlagUtils);
    }

    @Test
    void shouldReturnTrueWhenFeatureEnabledAndActivePaListIsEmpty() {
        when(featureFlagUtils.isPhysicalAddressLookupEnabled()).thenReturn(true);
        when(physicalAddressLookupParameter.getActivePAsForPhysicalAddressLookup()).thenReturn(Collections.emptyList());

        assertThat(physicalAddressLookupUtil.checkPhysicalAddressLookupIsEnabled("anyPaId")).isTrue();
    }

    @Test
    void shouldReturnTrueWhenFeatureEnabledAndPaIdIsInActiveList() {
        when(featureFlagUtils.isPhysicalAddressLookupEnabled()).thenReturn(true);
        when(physicalAddressLookupParameter.getActivePAsForPhysicalAddressLookup()).thenReturn(List.of("pa1", "pa2"));

        assertThat(physicalAddressLookupUtil.checkPhysicalAddressLookupIsEnabled("pa1")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenFeatureEnabledButPaIdIsNotInActiveList() {
        when(featureFlagUtils.isPhysicalAddressLookupEnabled()).thenReturn(true);
        when(physicalAddressLookupParameter.getActivePAsForPhysicalAddressLookup()).thenReturn(List.of("pa1", "pa2"));

        assertThat(physicalAddressLookupUtil.checkPhysicalAddressLookupIsEnabled("pa3")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenFeatureIsDisabled() {
        when(featureFlagUtils.isPhysicalAddressLookupEnabled()).thenReturn(false);
        when(physicalAddressLookupParameter.getActivePAsForPhysicalAddressLookup()).thenReturn(Collections.emptyList());

        assertThat(physicalAddressLookupUtil.checkPhysicalAddressLookupIsEnabled("anyPaId")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenFeatureIsDisabledEvenIfPaIdIsInActiveList() {
        when(featureFlagUtils.isPhysicalAddressLookupEnabled()).thenReturn(false);
        when(physicalAddressLookupParameter.getActivePAsForPhysicalAddressLookup()).thenReturn(List.of("pa1"));

        assertThat(physicalAddressLookupUtil.checkPhysicalAddressLookupIsEnabled("pa1")).isFalse();
    }
}