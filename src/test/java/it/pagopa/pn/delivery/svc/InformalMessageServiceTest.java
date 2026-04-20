package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InformalMessageServiceTest {
    @Mock
    private PnDataVaultClientImpl pnDataVaultClient;
    @Mock
    private PnDeliveryConfigs pnDeliveryConfigs;
    @InjectMocks
    private InformalMessageService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new InformalMessageService(pnDataVaultClient, pnDeliveryConfigs);
    }

    @Test
    void createInformalMessage_primaryLanguageNotIT_shouldThrow() {
        MessageRequestDto req = new MessageRequestDto();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage(LocalizedContent.LanguageEnum.FR);
        primary.setLongBody("test");
        req.setPrimaryContent(primary);
        when(pnDeliveryConfigs.getMaxMessageLongBodyLength()).thenReturn(100);
        when(pnDeliveryConfigs.getMaxMessageShortBodyLength()).thenReturn(100);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createInformalMessage(req));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void createInformalMessage_additionalLanguageInvalid_shouldThrow() {
        MessageRequestDto req = new MessageRequestDto();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage(LocalizedContent.LanguageEnum.IT);
        primary.setLongBody("test");
        req.setPrimaryContent(primary);
        LocalizedContent secondary = new LocalizedContent();
        secondary.setLanguage(LocalizedContent.LanguageEnum.IT);
        secondary.setLongBody("test");
        req.setSecondaryContent(secondary);
        when(pnDeliveryConfigs.getMaxMessageLongBodyLength()).thenReturn(100);
        when(pnDeliveryConfigs.getMaxMessageShortBodyLength()).thenReturn(100);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createInformalMessage(req));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void createInformalMessage_bodyLengthExceeded_shouldThrow() {
        MessageRequestDto req = new MessageRequestDto();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage(LocalizedContent.LanguageEnum.IT);
        primary.setLongBody("a".repeat(101));
        req.setPrimaryContent(primary);
        when(pnDeliveryConfigs.getMaxMessageLongBodyLength()).thenReturn(100);
        when(pnDeliveryConfigs.getMaxMessageShortBodyLength()).thenReturn(100);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createInformalMessage(req));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void createInformalMessage_validRequest_shouldCallClient() {
        MessageRequestDto req = new MessageRequestDto();
        LocalizedContent primary = new LocalizedContent();
        primary.setLanguage(LocalizedContent.LanguageEnum.IT);
        primary.setLongBody("test");
        req.setPrimaryContent(primary);
        when(pnDeliveryConfigs.getMaxMessageLongBodyLength()).thenReturn(100);
        when(pnDeliveryConfigs.getMaxMessageShortBodyLength()).thenReturn(100);
        MessageResponseDto resp = new MessageResponseDto();
        when(pnDataVaultClient.createInformalMessage(req)).thenReturn(resp);
        MessageResponseDto result = service.createInformalMessage(req);
        assertSame(resp, result);
        verify(pnDataVaultClient).createInformalMessage(req);
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
