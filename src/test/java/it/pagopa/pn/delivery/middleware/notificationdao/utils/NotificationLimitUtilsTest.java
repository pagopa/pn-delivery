package it.pagopa.pn.delivery.middleware.notificationdao.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationLimitUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "paId123, 2025-02-15T10:15:30+01:00, paId123##2025##02",
            "paId456, 2024-10-12T12:00:00+01:00, paId456##2024##10"
    })
    void createPrimaryKey_validInput(String paId, String sentAtStr, String expectedPk) {
        OffsetDateTime sentAt = OffsetDateTime.parse(sentAtStr);
        String result = NotificationLimitUtils.createPrimaryKey(paId, sentAt);

        assertEquals(expectedPk, result);
    }

    @ParameterizedTest
    @CsvSource({
            "2025-02-01T10:15:30+01:00, dailyCounter01",
            "2024-11-15T10:15:30+01:00, dailyCounter15",
            "2025-01-31T10:15:30+01:00, dailyCounter31"
    })
    void createDailyCounter_validInput(String sentAtStr, String expectedCounter) {
        OffsetDateTime sentAt = OffsetDateTime.parse(sentAtStr);
        String result = NotificationLimitUtils.createDailyCounter(sentAt);

        assertEquals(expectedCounter, result);
    }

}