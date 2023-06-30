package it.pagopa.pn.delivery.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataUtils {

    public static final String DELIMITER = "##";

    public static String createConcatenation(String... items) {
        return String.join(DELIMITER, items);
    }

    public static String extractIUN(String iunRecipientIdDelegateIdGroupId) {
        String[] splitIunRecipientIdDelegateIdGroupId = iunRecipientIdDelegateIdGroupId.split(DELIMITER);
        return splitIunRecipientIdDelegateIdGroupId[0];
    }

    public static String extractCreationMonth(Instant sentAt) {
        String sentAtString = sentAt.toString();
        String[] splitSentAt = sentAtString.split("-");
        return splitSentAt[0] + splitSentAt[1];
    }
}
