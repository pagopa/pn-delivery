package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PhysicalAddressLookupParameterConsumerTest {
    private ParameterConsumer parameterConsumer;
    private PhysicalAddressLookupParameterConsumer physicalAddressLookupParameterConsumer;

    @BeforeEach
    void setUp() {
        parameterConsumer = mock(ParameterConsumer.class);
        physicalAddressLookupParameterConsumer = new PhysicalAddressLookupParameterConsumer(parameterConsumer);
    }

    @Test
    void returnsEmptyListWhenNoActivePAsFound() {
        when(parameterConsumer.getParameterValue(anyString(), any())).thenReturn(Optional.empty());

        List<String> result = physicalAddressLookupParameterConsumer.getActivePAsForPhysicalAddressLookup();

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsListOfActivePAsWhenFound() {
        String[] activePAs = {"PA1", "PA2"};
        when(parameterConsumer.getParameterValue(anyString(), any())).thenReturn(Optional.of(activePAs));

        List<String> result = physicalAddressLookupParameterConsumer.getActivePAsForPhysicalAddressLookup();

        assertEquals(List.of("PA1", "PA2"), result);
    }

    @Test
    void returnsEmptyListWhenActivePAsArrayIsEmpty() {
        String[] activePAs = {};
        when(parameterConsumer.getParameterValue(anyString(), any())).thenReturn(Optional.of(activePAs));

        List<String> result = physicalAddressLookupParameterConsumer.getActivePAsForPhysicalAddressLookup();

        assertTrue(result.isEmpty());
    }
}