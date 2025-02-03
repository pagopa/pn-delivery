package it.pagopa.pn.delivery.middleware.notificationdao;

public interface PaNotificationLimitDao {
    void decrementLimitIncrementDailyCounter(String pk, String dailyCounter);
    void incrementLimitDecrementDailyCounter(String pk, String dailyCounter);
    boolean checkIfPaNotificationLimitExists(String pk);

}
