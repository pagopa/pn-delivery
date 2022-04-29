package it.pagopa.pn.delivery;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery")
@Data
public class PnDeliveryConfigs {

    private String externalChannelBaseUrl;

    private String deliveryPushBaseUrl;

    private String mandateBaseUrl;

    private Topics topics;

    private Duration preloadUrlDuration;

    private Duration downloadUrlDuration;

    private boolean downloadWithPresignedUrl;

    private Integer numberOfPresignedRequest;

    private NotificationDao notificationDao;

    private NotificationMetadataDao notificationMetadataDao;

    private Integer maxPageSize;

    @Data
    public static class Topics {
        private String newNotifications;
    }

    @Data
    public static class NotificationDao {
        private String tableName;
    }

    @Data
    public static class NotificationMetadataDao {
        private String tableName;
    }
}
