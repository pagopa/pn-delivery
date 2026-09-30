package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.exception.PnInvalidInputException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER;
import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_SIZE;

/**
 * Supporto alla paginazione "locale" della lista campagne: le campagne sono gia' tutte
 * disponibili in cache, quindi la chiave di pagina e' un offset opaco e versionato.
 */
public final class CampaignPaginationUtils {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MIN_PAGE_SIZE = 1;
    public static final int MAX_PAGE_SIZE = 50;

    private static final String CURSOR_VERSION = "v1";
    private static final String CURSOR_SEPARATOR = ":";
    private static final String SIZE_PARAM = "size";
    private static final String NEXT_PAGES_KEY_PARAM = "nextPagesKey";

    private CampaignPaginationUtils() {
    }

    public static int resolvePageSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_SIZE, SIZE_PARAM,
                    "size must be between " + MIN_PAGE_SIZE + " and " + MAX_PAGE_SIZE);
        }
        return size;
    }

    public static String encodeOffset(int offset) {
        String payload = CURSOR_VERSION + CURSOR_SEPARATOR + offset;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static int decodeOffset(String nextPagesKey) {
        if (nextPagesKey == null || nextPagesKey.isBlank()) {
            return 0;
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(nextPagesKey), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw invalidNextPagesKey();
        }

        String[] tokens = payload.split(CURSOR_SEPARATOR, 2);
        if (tokens.length != 2 || !CURSOR_VERSION.equals(tokens[0])) {
            throw invalidNextPagesKey();
        }

        int offset;
        try {
            offset = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            throw invalidNextPagesKey();
        }

        if (offset < 0) {
            throw invalidNextPagesKey();
        }
        return offset;
    }

    public static PnInvalidInputException invalidNextPagesKey() {
        return new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER, NEXT_PAGES_KEY_PARAM,
                "nextPagesKey is not a valid pagination key");
    }
}
