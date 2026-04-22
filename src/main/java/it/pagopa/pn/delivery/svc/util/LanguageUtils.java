package it.pagopa.pn.delivery.svc.util;

import it.pagopa.pn.delivery.svc.search.AllowedAdditionalLanguages;
import java.util.Arrays;

public class LanguageUtils {
    private LanguageUtils() {}

    public static boolean isValidAdditionalLanguage(String lang) {
        return Arrays.stream(AllowedAdditionalLanguages.values())
                .map(AllowedAdditionalLanguages::name)
                .anyMatch(lang::equals);
    }
}

