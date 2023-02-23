package it.pagopa.pn.delivery.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataUtils {

    public static String createConcatenation(String... items) {
        return String.join("##", items);
    }

    public static String extractCreationMonth(Instant sentAt) {
        String sentAtString = sentAt.toString();
        String[] splitSentAt = sentAtString.split( "-" );
        return splitSentAt[0] + splitSentAt[1];
    }

}
