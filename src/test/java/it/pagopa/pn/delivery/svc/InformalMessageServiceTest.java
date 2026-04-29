package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequest;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InformalMessageServiceTest {
    @Mock
    private PnDataVaultClientImpl pnDataVaultClient;
    @Mock
    private PnDeliveryConfigs pnDeliveryConfigs;

    private InformalMessageService service;

    @BeforeEach
    void setUp() {
        service = new InformalMessageService(pnDataVaultClient, pnDeliveryConfigs);
    }

    @Test
    void createInformalMessage_primaryLanguageNotIT_shouldThrow() {
        NewMessageRequest req = new NewMessageRequest();
        var primary = new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent();
        primary.setLanguage("FR");
        primary.setLongBody("test");
        req.setPrimaryMessage(primary);
        assertThrows(PnBadRequestException.class, () -> service.createInformalMessage(req, "senderId"));
    }

    @Test
    void createInformalMessage_additionalLanguageInvalid_shouldThrow() {
        NewMessageRequest req = new NewMessageRequest();
        var primary = new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("test");
        req.setPrimaryMessage(primary);
        var secondary = new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent();
        secondary.setLanguage("IT");
        secondary.setLongBody("test");
        req.setAdditionalMessage(secondary);
        assertThrows(PnBadRequestException.class, () -> service.createInformalMessage(req, "senderId"));
    }

    @Test
    void createInformalMessage_bodyLengthExceeded_shouldThrow() {
        NewMessageRequest req = new NewMessageRequest();
        var primary = new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("a".repeat(101));
        req.setPrimaryMessage(primary);
        assertThrows(PnBadRequestException.class, () -> service.createInformalMessage(req, "senderId"));
    }

    @Test
    void createInformalMessage_validRequest_shouldCallClient() {
        NewMessageRequest req = new NewMessageRequest();
        var primary = new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent();
        primary.setLanguage("IT");
        primary.setLongBody("test");
        req.setPrimaryMessage(primary);
        when(pnDeliveryConfigs.getMaxMessageLongBodyLength()).thenReturn(100);
        when(pnDeliveryConfigs.getMaxMessageShortBodyLength()).thenReturn(100);
        MessageResponseDto resp = new MessageResponseDto();
        // Il mock del client va fatto su qualsiasi MessageRequestDto
        when(pnDataVaultClient.createInformalMessage(any())).thenReturn(resp);
        MessageResponseDto result = service.createInformalMessage(req, "senderId");
        assertSame(resp, result);
        verify(pnDataVaultClient).createInformalMessage(any());
    }

    @Test
    void getInformalMessageById_shouldCallClient() {
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        MessageResponseDto resp = new MessageResponseDto();
        when(pnDataVaultClient.getInformalMessageById(messageId, senderId)).thenReturn(resp);
        MessageResponseDto result = service.getInformalMessageById(messageId, senderId);
        assertSame(resp, result);
        verify(pnDataVaultClient).getInformalMessageById(messageId, senderId);
    }
}
