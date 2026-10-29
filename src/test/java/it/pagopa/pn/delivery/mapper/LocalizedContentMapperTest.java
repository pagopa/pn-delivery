package it.pagopa.pn.delivery.mapper;

import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequestAdditionalMessage;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequestPrimaryMessage;
import it.pagopa.pn.delivery.models.internal.notification.mapper.LocalizedContentMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LocalizedContentMapperTest {

    @Test
    void mapToServerLocalizedContent_shouldReturnNull_whenSourceIsNull() {
        NewMessageRequestPrimaryMessage result = LocalizedContentMapper.mapToServerPrimaryMessage(null);

        Assertions.assertNull(result);
    }

    @Test
    void mapToServerLocalizedContent_shouldMapAllFieldsPrimaryMessage() {
        LocalizedContent source = new LocalizedContent();

        source.setLanguage(LocalizedContent.LanguageEnum.IT);
        source.setSubject("Oggetto");
        source.setShortBody("Messaggio breve");
        source.setLongBody("Messaggio lungo");

        NewMessageRequestPrimaryMessage result = LocalizedContentMapper.mapToServerPrimaryMessage(source);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("IT", result.getLanguage());
        Assertions.assertEquals("Oggetto", result.getSubject());
        Assertions.assertEquals("Messaggio breve", result.getShortBody());
        Assertions.assertEquals("Messaggio lungo", result.getLongBody());
    }

    @Test
    void mapToServerPrimaryMessage_shouldThrowException_whenRequiredFieldsAreNull() {
        LocalizedContent source = new LocalizedContent();

        Assertions.assertThrows(
                NullPointerException.class,
                () -> LocalizedContentMapper.mapToServerPrimaryMessage(source)
        );
    }

    @Test
    void mapToServerAdditionalMessage_shouldMapAllFieldsAdditionalMessage() {
        LocalizedContent source = new LocalizedContent();

        source.setLanguage(LocalizedContent.LanguageEnum.IT);
        source.setSubject("Oggetto");
        source.setShortBody("Messaggio breve");
        source.setLongBody("Messaggio lungo");

        NewMessageRequestAdditionalMessage result = LocalizedContentMapper.mapToServerAdditionalMessage(source);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("IT", result.getLanguage());
        Assertions.assertEquals("Oggetto", result.getSubject());
        Assertions.assertEquals("Messaggio breve", result.getShortBody());
        Assertions.assertEquals("Messaggio lungo", result.getLongBody());
    }

    @Test
    void mapToServerAdditionalMessage_shouldThrowException_whenRequiredFieldsAreNull() {
        LocalizedContent source = new LocalizedContent();

        Assertions.assertThrows(
                NullPointerException.class,
                () -> LocalizedContentMapper.mapToServerAdditionalMessage(source)
        );
    }
}