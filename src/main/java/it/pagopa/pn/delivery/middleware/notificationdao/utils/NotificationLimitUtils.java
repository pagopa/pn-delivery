package it.pagopa.pn.delivery.middleware.notificationdao.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationLimitUtils {

    private static final String DAILY_COUNTER = "dailyCounter";

    public static String createPrimaryKey(String paId, OffsetDateTime sentAt) {
        log.info("Creating primary key for paId: {} and sentAt: {}", paId, sentAt);
        return paId + "##" + sentAt.getYear() + "##" + String.format("%02d", sentAt.getMonthValue());
    }

    public static String createDailyCounter(OffsetDateTime sentAt) {
        String dailyCounter = DAILY_COUNTER + String.format("%02d", sentAt.getDayOfMonth());
        log.info("Creating daily counter: {}", dailyCounter);
        return dailyCounter;
    }
}
