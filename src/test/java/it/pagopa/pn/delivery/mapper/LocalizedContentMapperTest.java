package it.pagopa.pn.delivery.mapper;

import it.pagopa.pn.delivery.models.internal.notification.mapper.LocalizedContentMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LocalizedContentMapperTest {

    @Test
    void mapToServerLocalizedContent_shouldReturnNull_whenSourceIsNull() {
        it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent result =
                LocalizedContentMapper.mapToServerLocalizedContent(null);

        Assertions.assertNull(result);
    }

    @Test
    void mapToServerLocalizedContent_shouldMapAllFields() {
        it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent source =
                new it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent();

        source.setLanguage(
                it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent.LanguageEnum.IT
        );
        source.setSubject("Oggetto");
        source.setShortBody("Messaggio breve");
        source.setLongBody("Messaggio lungo");

        it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent result =
                LocalizedContentMapper.mapToServerLocalizedContent(source);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("IT", result.getLanguage());
        Assertions.assertEquals("Oggetto", result.getSubject());
        Assertions.assertEquals("Messaggio breve", result.getShortBody());
        Assertions.assertEquals("Messaggio lungo", result.getLongBody());
    }

    @Test
    void mapToServerLocalizedContent_shouldThrowException_whenRequiredFieldsAreNull() {
        it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent source =
                new it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent();

        Assertions.assertThrows(
                NullPointerException.class,
                () -> LocalizedContentMapper.mapToServerLocalizedContent(source)
        );
    }
}