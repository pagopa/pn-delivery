package it.pagopa.pn.delivery.rest.mapper;

import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.MessageResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequest;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InformalMessageMapperTest {
    @Test
    void testToMsAndToApiLocalizedContent() {
        // from API to MS
        var apiContent = new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent();
        apiContent.setSubject("subject");
        apiContent.setLongBody("longBody");
        apiContent.setShortBody("shortBody");
        apiContent.setLanguage("IT");
        LocalizedContent msContent = InformalMessageMapper.toMs(apiContent);
        assertEquals("subject", msContent.getSubject());
        assertEquals("longBody", msContent.getLongBody());
        assertEquals("shortBody", msContent.getShortBody());
        assertEquals(LocalizedContent.LanguageEnum.IT, msContent.getLanguage());

        // from MS to API
        var apiContent2 = InformalMessageMapper.toApi(msContent);
        assertEquals("subject", apiContent2.getSubject());
        assertEquals("longBody", apiContent2.getLongBody());
        assertEquals("shortBody", apiContent2.getShortBody());
        assertEquals("IT", apiContent2.getLanguage());
    }

    @Test
    void testToMsFromNewMessageRequest() {
        var apiContent = new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent();
        apiContent.setSubject("subject");
        apiContent.setLongBody("longBody");
        apiContent.setShortBody("shortBody");
        apiContent.setLanguage("DE");
        var req = new NewMessageRequest();
        req.setPrimaryMessage(apiContent);
        req.setAdditionalMessage(null);
        MessageRequestDto ms = InformalMessageMapper.toMs(req, "senderId");
        assertEquals("senderId", ms.getSenderId());
        assertNotNull(ms.getPrimaryContent());
        assertNull(ms.getSecondaryContent());
        assertEquals("subject", ms.getPrimaryContent().getSubject());
        assertEquals(LocalizedContent.LanguageEnum.DE, ms.getPrimaryContent().getLanguage());
    }

    @Test
    void testToApiFromMessageResponseDto() {
        LocalizedContent msContent = new LocalizedContent();
        UUID messageId=UUID.randomUUID();
        msContent.setSubject("subj");
        msContent.setLongBody("body");
        msContent.setShortBody("short");
        msContent.setLanguage(LocalizedContent.LanguageEnum.FR);
        MessageResponseDto msDto = new MessageResponseDto();
        msDto.setMessageId(messageId);
        msDto.setPrimaryContent(msContent);
        msDto.setSecondaryContent(null);
        msDto.setCreatedAt(OffsetDateTime.now());
        MessageResponse api = InformalMessageMapper.toApi(msDto);
        assertEquals(messageId, api.getMessageId());
        assertNotNull(api.getPrimaryMessage());
        assertNull(api.getAdditionalMessage());
        assertEquals(msDto.getCreatedAt(), api.getCreatedAt());
        assertEquals("subj", api.getPrimaryMessage().getSubject());
        assertEquals("FR", api.getPrimaryMessage().getLanguage());
    }
}

