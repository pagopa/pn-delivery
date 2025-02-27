package it.pagopa.pn.delivery.middleware.notificationdao;

import java.time.OffsetDateTime;

public interface PaNotificationLimitDao {
    boolean decrementLimitIncrementDailyCounter(String paId, OffsetDateTime sentAt);
    void incrementLimitDecrementDailyCounter(String paId, OffsetDateTime sentAt);
    boolean checkIfPaNotificationLimitExists(String paId, OffsetDateTime sentAt);

}
