package it.pagopa.pn.delivery.utils;


import lombok.extern.slf4j.Slf4j;

import javax.el.PropertyNotFoundException;


@Slf4j
public enum DenominationValidationUtils {
    ISO_LATIN_1("^[\\u0020-\\u007E\\u00A0-\\u00FF]+$");

    private String regex;

    DenominationValidationUtils(String regex) {
        this.regex =regex;
    }

    public static String getRegexValue(String name){
        try {
            return DenominationValidationUtils.valueOf(name.trim().toUpperCase()).regex;
        }catch(IllegalArgumentException iae) {
            log.error("NO VALUE FOR DENOMINATION VALIDATION VALUE: {}",name);
            throw new PropertyNotFoundException("NO VALUE FOR DENOMINATION VALIDATION VALUE: "+name);
        }
    }

    public enum ValidationTypeAllowedValues {
        REGEX, NONE;
    }
}
