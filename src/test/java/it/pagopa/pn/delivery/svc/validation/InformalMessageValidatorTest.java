package it.pagopa.pn.delivery.svc.validation;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InformalMessageValidatorTest {
    private PnDeliveryConfigs pnDeliveryConfigs;

    @BeforeEach
    void setUp() {
        pnDeliveryConfigs = mock(PnDeliveryConfigs.class);
        when(pnDeliveryConfigs.getMaxMessageLongBodyLength()).thenReturn(100);
        when(pnDeliveryConfigs.getMaxMessageShortBodyLength()).thenReturn(100);
    }

    @Test
    void validate_nullRequest_shouldThrow() {
        assertThrows(PnBadRequestException.class, () ->
                InformalMessageValidator.validate(null, pnDeliveryConfigs));
    }


    @Test
    void validate_primaryLanguageNotIT_shouldThrow() {
        NewMessageRequest req = new NewMessageRequest();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage("FR");
        primary.setLongBody("test");
        req.setPrimaryMessage(primary);
        assertThrows(PnBadRequestException.class, () ->
                InformalMessageValidator.validate(req, pnDeliveryConfigs));
    }

    @Test
    void validate_invalidAdditionalLanguage_shouldThrow() {
        NewMessageRequest req = new NewMessageRequest();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("test");
        req.setPrimaryMessage(primary);
        LocalizedContent secondary = new LocalizedContent();
        secondary.setLanguage("IT"); // non valido come additional
        secondary.setLongBody("test");
        req.setAdditionalMessage(secondary);
        assertThrows(PnBadRequestException.class, () ->
                InformalMessageValidator.validate(req, pnDeliveryConfigs));
    }

    @Test
    void validate_bodyLengthExceeded_shouldThrow() {
        NewMessageRequest req = new NewMessageRequest();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("a".repeat(101));
        req.setPrimaryMessage(primary);
        assertThrows(PnBadRequestException.class, () ->
                InformalMessageValidator.validate(req, pnDeliveryConfigs));
    }

    @Test
    void validate_validRequest_shouldNotThrow() {
        NewMessageRequest req = new NewMessageRequest();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("test");
        req.setPrimaryMessage(primary);
        assertDoesNotThrow(() -> InformalMessageValidator.validate(req, pnDeliveryConfigs));
    }

    @Test
    void validate_validRequestWithAdditional_shouldNotThrow() {
        NewMessageRequest req = new NewMessageRequest();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("test");
        req.setPrimaryMessage(primary);
        LocalizedContent secondary = new LocalizedContent();
        secondary.setLanguage("DE"); // valido
        secondary.setLongBody("test");
        req.setAdditionalMessage(secondary);
        assertDoesNotThrow(() -> InformalMessageValidator.validate(req, pnDeliveryConfigs));
    }

    @Test
    void validate_primaryLongBodyNotNull_shouldNotThrow() {
        NewMessageRequest req = new NewMessageRequest();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("test long body");
        req.setPrimaryMessage(primary);
        assertDoesNotThrow(() -> InformalMessageValidator.validate(req, pnDeliveryConfigs));
    }

    @Test
    void validate_secondaryLongBodyNotNull_shouldNotThrow() {
        NewMessageRequest req = new NewMessageRequest();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("test long body");
        req.setPrimaryMessage(primary);
        LocalizedContent secondary = new LocalizedContent();
        secondary.setLanguage("DE");
        secondary.setLongBody("test secondary long body");
        req.setAdditionalMessage(secondary);
        assertDoesNotThrow(() -> InformalMessageValidator.validate(req, pnDeliveryConfigs));
    }

    @Test
    void validate_primaryShortBodyNotNull_shouldNotThrow() {
        NewMessageRequest req = new NewMessageRequest();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("test");
        primary.setShortBody("short");
        req.setPrimaryMessage(primary);
        assertDoesNotThrow(() -> InformalMessageValidator.validate(req, pnDeliveryConfigs));
    }

    @Test
    void validate_secondaryShortBodyNotNull_shouldNotThrow() {
        NewMessageRequest req = new NewMessageRequest();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("test");
        req.setPrimaryMessage(primary);
        LocalizedContent secondary = new LocalizedContent();
        secondary.setLanguage("DE");
        secondary.setLongBody("test");
        secondary.setShortBody("short");
        req.setAdditionalMessage(secondary);
        assertDoesNotThrow(() -> InformalMessageValidator.validate(req, pnDeliveryConfigs));
    }

    @Test
    void validate_primaryShortBodyPresentButSecondaryMissing_shouldThrow() {
        NewMessageRequest req = new NewMessageRequest();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("test");
        primary.setShortBody("short");
        req.setPrimaryMessage(primary);
        LocalizedContent secondary = new LocalizedContent();
        secondary.setLanguage("DE");
        secondary.setLongBody("test");
        secondary.setShortBody(null);
        req.setAdditionalMessage(secondary);
        assertThrows(PnBadRequestException.class, () ->
        InformalMessageValidator.validate(req, pnDeliveryConfigs));
    }
}
