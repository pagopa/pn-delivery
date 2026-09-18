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
    private static final String LEGAL_PARAMETER_NAME = "PaActiveForPhysicalAddressLookup";
    private static final String INFORMAL_PARAMETER_NAME = "InformalPaActiveForPhysicalAddressLookup";

    private ParameterConsumer parameterConsumer;
    private PhysicalAddressLookupParameterConsumer physicalAddressLookupParameterConsumer;

    @BeforeEach void setUp() {
        parameterConsumer = mock(ParameterConsumer.class);
        physicalAddressLookupParameterConsumer = new PhysicalAddressLookupParameterConsumer(parameterConsumer);
    }

    @Test void getActivePAsForPhysicalAddressLookupReturnsEmptyListWhenParameterIsMissing() {
        when(parameterConsumer.getParameterValue(eq(LEGAL_PARAMETER_NAME), eq(String[].class)))
                .thenReturn(Optional.empty());

        List<String> result = physicalAddressLookupParameterConsumer.getActivePAsForPhysicalAddressLookup();

        assertTrue(result.isEmpty());
        verify(parameterConsumer).getParameterValue(eq(LEGAL_PARAMETER_NAME), eq(String[].class));
    }

    @Test void getActivePAsForPhysicalAddressLookupReturnsListWhenParameterExists() {
        when(parameterConsumer.getParameterValue(eq(LEGAL_PARAMETER_NAME), eq(String[].class)))
                .thenReturn(Optional.of(new String[]{"PA1", "PA2"}));

        List<String> result = physicalAddressLookupParameterConsumer.getActivePAsForPhysicalAddressLookup();

        assertEquals(List.of("PA1", "PA2"), result);
        verify(parameterConsumer).getParameterValue(eq(LEGAL_PARAMETER_NAME), eq(String[].class));
    }

    @Test void getActivePAsForPhysicalAddressLookupReturnsEmptyListWhenArrayIsEmpty() {
        when(parameterConsumer.getParameterValue(eq(LEGAL_PARAMETER_NAME), eq(String[].class)))
                .thenReturn(Optional.of(new String[]{}));

        List<String> result = physicalAddressLookupParameterConsumer.getActivePAsForPhysicalAddressLookup();

        assertTrue(result.isEmpty());
        verify(parameterConsumer).getParameterValue(eq(LEGAL_PARAMETER_NAME), eq(String[].class));
    }

    @Test void getInformalActivePAsForPhysicalAddressLookupReturnsEmptyListWhenParameterIsMissing() {
        when(parameterConsumer.getParameterValue(eq(INFORMAL_PARAMETER_NAME), eq(String[].class)))
                .thenReturn(Optional.empty());

        List<String> result = physicalAddressLookupParameterConsumer.getInformalActivePAsForPhysicalAddressLookup();

        assertTrue(result.isEmpty());
        verify(parameterConsumer).getParameterValue(eq(INFORMAL_PARAMETER_NAME), eq(String[].class));
    }

    @Test void getInformalActivePAsForPhysicalAddressLookupReturnsListWhenParameterExists() {
        when(parameterConsumer.getParameterValue(eq(INFORMAL_PARAMETER_NAME), eq(String[].class)))
                .thenReturn(Optional.of(new String[]{"IPA1", "IPA2"}));

        List<String> result = physicalAddressLookupParameterConsumer.getInformalActivePAsForPhysicalAddressLookup();

        assertEquals(List.of("IPA1", "IPA2"), result);
        verify(parameterConsumer).getParameterValue(eq(INFORMAL_PARAMETER_NAME), eq(String[].class));
    }

    @Test void getInformalActivePAsForPhysicalAddressLookupReturnsEmptyListWhenArrayIsEmpty() {
        when(parameterConsumer.getParameterValue(eq(INFORMAL_PARAMETER_NAME), eq(String[].class)))
                .thenReturn(Optional.of(new String[]{}));

        List<String> result = physicalAddressLookupParameterConsumer.getInformalActivePAsForPhysicalAddressLookup();

        assertTrue(result.isEmpty());
        verify(parameterConsumer).getParameterValue(eq(INFORMAL_PARAMETER_NAME), eq(String[].class));
    }
}