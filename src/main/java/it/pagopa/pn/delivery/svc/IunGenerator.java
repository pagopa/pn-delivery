package it.pagopa.pn.delivery.svc;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

public class IunGenerator {

    private static final char[] IUN_CHARS = new char[] {'A','D','E','G','H','J','K','L','M','N','P','Q','R','T','U','V','W','X','Y','Z'};
    private static final List<String> INVALID_PAIRS = List.of("UV", "VU", "HN", "NH", "LJ");
    private static final String SEPARATOR = "-";

    private final SecureRandom randomNumberGenerator = new SecureRandom();

    public String generatePredictedIun( Instant creationInstant) {
        String creationDate = creationInstant.toString();
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
        StringBuilder buffer = new StringBuilder((segmentLength + 1) * segmentQuantity);
        for (int s = 0 ; s < segmentQuantity; s++) {
            if (s > 0) {
                buffer.append(separator);
            }
            int i = 0;
            while (i < segmentLength) {
                int randomLimitedInt = randomNumberGenerator.nextInt(IUN_CHARS.length);
                char currentChar = IUN_CHARS[randomLimitedInt];

                if (buffer.length() > 0) {
                    char prevChar = buffer.charAt( buffer.length() - 1 );
                    if ( isValidPair(currentChar, prevChar) ) {
                        buffer.append( currentChar );
                        i++;
                    }
                    // else ignore generated character
                }
                else {
                    buffer.append( currentChar );
                    i++;
                }


            }
        }
        return buffer.toString();
    }

    private boolean isValidPair(char currentChar, char prevChar) {
        boolean valid = true;
        if (currentChar != prevChar) {
            String pair = prevChar + "" + currentChar;
            if ( INVALID_PAIRS.contains( pair ) ) {
                valid = false;
            }
        } else {
            valid = false;
        }
        return valid;
    }
}
