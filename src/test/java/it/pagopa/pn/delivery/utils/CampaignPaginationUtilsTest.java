package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    @Test
    void decodeOffset_rejectsUnknownVersion() {
        String key = encode("v2:10");
        Assertions.assertThrows(PnInvalidInputException.class, () -> CampaignPaginationUtils.decodeOffset(key));
    }

    @Test
    void decodeOffset_rejectsMalformedPayload() {
        String key = encode("10");
        Assertions.assertThrows(PnInvalidInputException.class, () -> CampaignPaginationUtils.decodeOffset(key));
    }

    @Test
    void decodeOffset_rejectsNonNumericOffset() {
        String key = encode("v1:abc");
        Assertions.assertThrows(PnInvalidInputException.class, () -> CampaignPaginationUtils.decodeOffset(key));
    }

    @Test
    void decodeOffset_rejectsNegativeOffset() {
        String key = encode("v1:-1");
        Assertions.assertThrows(PnInvalidInputException.class, () -> CampaignPaginationUtils.decodeOffset(key));
    }

    private String encode(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
