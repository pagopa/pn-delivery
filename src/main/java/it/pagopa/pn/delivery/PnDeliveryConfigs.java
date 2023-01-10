package it.pagopa.pn.delivery;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery")
@Data
@Import(SharedAutoConfiguration.class)
public class PnDeliveryConfigs {

    private String externalChannelBaseUrl;

    private String deliveryPushBaseUrl;

    private String mandateBaseUrl;

    private String dataVaultBaseUrl;

    private String safeStorageBaseUrl;

    private String safeStorageCxId;

    private String externalRegistriesBaseUrl;

    private Topics topics;

    private Duration preloadUrlDuration;

    private Duration downloadUrlDuration;

    private boolean downloadWithPresignedUrl;

    private Integer numberOfPresignedRequest;

    private NotificationDao notificationDao;

    private NotificationMetadataDao notificationMetadataDao;

    private NotificationDelegationMetadataDao notificationDelegationMetadataDao;

    private NotificationCostDao notificationCostDao;

    private NotificationQRDao notificationQRDao;

    private Integer maxPageSize;

    private Costs costs;

    private String maxDocumentsAvailableDays;

    private String maxFirstNoticeCodeDays;

    private String maxSecondNoticeCodeDays;

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

    @Data
    public static class NotificationDelegationMetadataDao {
        private String tableName;
    }

    @Data
    public static class NotificationCostDao {
        private String tableName;
    }

    @Data
    public static class NotificationQRDao {
        private String tableName;
    }

    @Data
    public static class Costs {
        private String notification;
        private String raccomandataIta;
        private String raccomandataEstZona1;
        private String raccomandataEstZona2;
        private String raccomandataEstZona3;
    }
}
