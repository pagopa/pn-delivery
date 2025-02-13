package it.pagopa.pn.delivery.middleware.notificationdao.utils;

import it.pagopa.pn.delivery.middleware.notificationdao.entities.PaNotificationLimitEntity;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
public class NotificationLimitUtils {

    private static final String DAILY_COUNTER = "dailyCounter";

    public static Map<String, AttributeValue> createPrimaryKey(String paId, OffsetDateTime sentAt) {
        String pk = paId + "##" + sentAt.getYear() + "##" + String.format("%02d", sentAt.getMonthValue());
        log.info("Creating primary key: {}", pk);
        return Map.of(PaNotificationLimitEntity.FIELD_PK, AttributeValue.builder().s(pk).build());
    }

    public static String createDailyCounter(OffsetDateTime sentAt) {
        String dailyCounter = DAILY_COUNTER + String.format("%02d", sentAt.getDayOfMonth());
        log.info("Creating daily counter: {}", dailyCounter);
        return dailyCounter;
    }
}
