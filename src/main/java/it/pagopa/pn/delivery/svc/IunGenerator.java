package it.pagopa.pn.delivery.svc;

import java.util.Random;

public class IunGenerator {

    private static final char[] IUN_CHARS = new char[] {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
    private static final String SEPARATOR = "-";

    public String generatePredictedIun(String creationDate) {
        String[] creationDateSplit = creationDate.split( SEPARATOR );
        String randStringPart = generateRandomString(4, 3, '-');
        String monthPart = creationDateSplit[0] + creationDateSplit[1];
        char controlChar = generateControlChar( randStringPart, monthPart );
        return randStringPart + SEPARATOR + monthPart + SEPARATOR + controlChar + SEPARATOR + "1";
    }

    protected char generateControlChar(String randStringPart, String monthPart) {
        int sum=0;
        for (int i = 0; i < randStringPart.length(); i++) {
            char singleChar = randStringPart.charAt( i );
            sum += new String(IUN_CHARS).indexOf( singleChar ) + 1;
        }
        for (int i = 0; i < monthPart.length(); i++) {
            char singleChar = monthPart.charAt( i );
            sum += Integer.parseInt(String.valueOf(singleChar));
        }
        int mod = (sum % IUN_CHARS.length);
        return IUN_CHARS[mod];
    }

    private String generateRandomString(int segmentLength, int segmentQuantity, char separator) {
        Random random = new Random();
        StringBuilder buffer = new StringBuilder((segmentLength + 1) * segmentQuantity);
        for (int s = 0 ; s < segmentQuantity; s++) {
            if (s > 0) {
                buffer.append(separator);
            }
            for (int i = 0; i < segmentLength; i++) {
                int randomLimitedInt = random.nextInt(IUN_CHARS.length);
                buffer.append(IUN_CHARS[randomLimitedInt]);
            }
        }
        return buffer.toString();
    }
}
