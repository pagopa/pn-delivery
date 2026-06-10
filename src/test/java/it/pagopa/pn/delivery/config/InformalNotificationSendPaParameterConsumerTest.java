package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

class InformalNotificationSendPaParameterConsumerTest {

    public static final String CXID_ACTIVE = "PA-ACTIVE-001";
    public static final String CXID_INACTIVE = "PA-INACTIVE-002";
    public static final String CXID_NOT_IN_MAP = "PA-NOT-FOUND-003";

    private ParameterConsumer parameterConsumer;
    private InformalNotificationSendPaParameterConsumer informalNotificationSendPaParameterConsumer;

    @BeforeEach
    void setup() {
        this.parameterConsumer = Mockito.mock(ParameterConsumer.class);
    }

    @Test
    void testIsSenderActiveForInformalNotificationWithActiveConfig() {
        // Given
        InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive[] configs =
                new InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive[2];
        configs[0] = new InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive(CXID_ACTIVE, true);
        configs[1] = new InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive(CXID_INACTIVE, false);

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(configs));

        this.informalNotificationSendPaParameterConsumer =
                new InformalNotificationSendPaParameterConsumer(parameterConsumer);
        informalNotificationSendPaParameterConsumer.initialize();

        // When & Then
        Assertions.assertTrue(informalNotificationSendPaParameterConsumer.isSenderActiveForInformalNotification(CXID_ACTIVE));
        Assertions.assertFalse(informalNotificationSendPaParameterConsumer.isSenderActiveForInformalNotification(CXID_INACTIVE));
    }

    @Test
    void testIsSenderActiveForInformalNotificationWithDefaultValue() {
        // Given
        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.empty());

        this.informalNotificationSendPaParameterConsumer =
                new InformalNotificationSendPaParameterConsumer(parameterConsumer);
        // Simula il valore di default come false (dovrebbe essere iniettato da @Value)
        java.lang.reflect.Field field;
        try {
            field = InformalNotificationSendPaParameterConsumer.class.getDeclaredField("isSendInformalActiveDefaultValue");
            field.setAccessible(true);
            field.set(informalNotificationSendPaParameterConsumer, false);
        } catch (Exception e) {
            Assertions.fail("Unable to set default value");
        }

        informalNotificationSendPaParameterConsumer.initialize();

        // When & Then - dovrebbe usare il valore di default
        Assertions.assertFalse(informalNotificationSendPaParameterConsumer.isSenderActiveForInformalNotification(CXID_NOT_IN_MAP));
    }

    @Test
    void testIsSenderActiveForInformalNotificationNotFoundInMapReturnsDefault() {
        // Given
        InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive[] configs =
                new InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive[1];
        configs[0] = new InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive(CXID_ACTIVE, true);

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(configs));

        this.informalNotificationSendPaParameterConsumer =
                new InformalNotificationSendPaParameterConsumer(parameterConsumer);

        // Imposta il valore di default a true
        try {
            java.lang.reflect.Field field = InformalNotificationSendPaParameterConsumer.class
                    .getDeclaredField("isSendInformalActiveDefaultValue");
            field.setAccessible(true);
            field.set(informalNotificationSendPaParameterConsumer, true);
        } catch (Exception e) {
            Assertions.fail("Unable to set default value");
        }

        informalNotificationSendPaParameterConsumer.initialize();

        // When & Then
        Assertions.assertTrue(informalNotificationSendPaParameterConsumer.isSenderActiveForInformalNotification(CXID_ACTIVE));
        Assertions.assertTrue(informalNotificationSendPaParameterConsumer.isSenderActiveForInformalNotification(CXID_NOT_IN_MAP));
    }

    @Test
    void testInitializeWithEmptyOptional() {
        // Given
        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.empty());

        this.informalNotificationSendPaParameterConsumer =
                new InformalNotificationSendPaParameterConsumer(parameterConsumer);

        // When
        informalNotificationSendPaParameterConsumer.initialize();

        // Then - non dovrebbe sollevare eccezioni
        Assertions.assertNotNull(informalNotificationSendPaParameterConsumer);
    }

    @Test
    void testInitializeSkipsInvalidEntries() {
        // Given
        InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive[] configs =
                new InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive[4];
        configs[0] = new InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive(CXID_ACTIVE, true);
        configs[1] = new InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive(null, true); // Invalid: null cxId
        configs[2] = new InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive("", false); // Invalid: blank cxId
        configs[3] = new InformalNotificationSendPaParameterConsumer.CxIdIsInformalActive(CXID_INACTIVE, null); // Invalid: null isActive

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(configs));

        this.informalNotificationSendPaParameterConsumer =
                new InformalNotificationSendPaParameterConsumer(parameterConsumer);

        try {
            java.lang.reflect.Field field = InformalNotificationSendPaParameterConsumer.class
                    .getDeclaredField("isSendInformalActiveDefaultValue");
            field.setAccessible(true);
            field.set(informalNotificationSendPaParameterConsumer, false);
        } catch (Exception e) {
            Assertions.fail("Unable to set default value");
        }

        informalNotificationSendPaParameterConsumer.initialize();

        // When & Then - solo il primo elemento valido dovrebbe essere presente
        Assertions.assertTrue(informalNotificationSendPaParameterConsumer.isSenderActiveForInformalNotification(CXID_ACTIVE));
        // Gli altri cxId non dovrebbero essere in mappa, quindi restituiscono il default
        Assertions.assertFalse(informalNotificationSendPaParameterConsumer.isSenderActiveForInformalNotification(CXID_INACTIVE));
    }
}
