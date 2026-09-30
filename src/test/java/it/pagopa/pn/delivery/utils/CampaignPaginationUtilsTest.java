package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class CampaignPaginationUtilsTest {

    @Test
    void resolvePageSize_defaultWhenNull() {
        Assertions.assertEquals(CampaignPaginationUtils.DEFAULT_PAGE_SIZE,
                CampaignPaginationUtils.resolvePageSize(null));
    }

    @Test
    void resolvePageSize_acceptsBoundaries() {
        Assertions.assertEquals(1, CampaignPaginationUtils.resolvePageSize(1));
        Assertions.assertEquals(50, CampaignPaginationUtils.resolvePageSize(50));
    }

    @Test
    void resolvePageSize_rejectsOutOfRange() {
        Assertions.assertThrows(PnInvalidInputException.class, () -> CampaignPaginationUtils.resolvePageSize(0));
        Assertions.assertThrows(PnInvalidInputException.class, () -> CampaignPaginationUtils.resolvePageSize(51));
    }

    @Test
    void encodeDecode_roundTrip() {
        String key = CampaignPaginationUtils.encodeOffset(42);
        Assertions.assertEquals(42, CampaignPaginationUtils.decodeOffset(key));
    }

    @Test
    void encodeOffset_isOpaque() {
        String key = CampaignPaginationUtils.encodeOffset(10);
        Assertions.assertFalse(key.contains("10"));
    }

    @Test
    void decodeOffset_nullOrBlankMeansFirstPage() {
        Assertions.assertEquals(0, CampaignPaginationUtils.decodeOffset(null));
        Assertions.assertEquals(0, CampaignPaginationUtils.decodeOffset("   "));
    }

    @Test
    void decodeOffset_rejectsNotBase64() {
        Assertions.assertThrows(PnInvalidInputException.class,
                () -> CampaignPaginationUtils.decodeOffset("not-base64!!"));
    }

    @ParameterizedTest(name = "[{index}] payload \"{0}\"")
    @ValueSource(strings = {
            "v2:10",  // versione sconosciuta
            "10",     // payload malformato
            "v1:abc", // offset non numerico
            "v1:-1"   // offset negativo
    })
    void decodeOffset_rejectsInvalidPayload(String payload) {
        String key = encode(payload);
        Assertions.assertThrows(PnInvalidInputException.class, () -> CampaignPaginationUtils.decodeOffset(key));
    }

    private String encode(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
