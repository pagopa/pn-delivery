package it.pagopa.pn.delivery;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery")
@Data
@EnableScheduling
@Import(SharedAutoConfiguration.class)
@Slf4j
public class PnDeliveryConfigs {

    private String externalChannelBaseUrl;

    private String deliveryPushBaseUrl;

    private String mandateBaseUrl;

    private String dataVaultBaseUrl;

    private String safeStorageBaseUrl;

    private String nationalRegistriesBaseUrl;

    private String f24BaseUrl;

    private String safeStorageCxId;
    private String f24CxId;

    private String externalRegistriesBaseUrl;

    private Topics topics;

    private Duration preloadUrlDuration;

    private Duration downloadUrlDuration;

    private boolean downloadWithPresignedUrl;

    private Integer numberOfPresignedRequest;

    private NotificationDao notificationDao;

    private NotificationMetadataDao notificationMetadataDao;

    private TaxonomyCodeDao taxonomyCodeDao;

    private NotificationDelegationMetadataDao notificationDelegationMetadataDao;

    private NotificationCostDao notificationCostDao;

    private NotificationQRDao notificationQRDao;

    private PaNotificationLimitDao paNotificationLimitDao;

    private NotificationRefusedVerificationDao notificationRefusedVerificationDao;

    private Integer maxPageSize;

    private String maxDocumentsAvailableDays;

    private String maxFirstNoticeCodeDays;

    private String maxSecondNoticeCodeDays;

    private Integer maxRecipientsCount;

    private Integer maxAttachmentsCount;

    private Integer searchTimeoutSeconds;

    private boolean physicalAddressValidation = false;
    
    private String physicalAddressValidationPattern;

    private Integer physicalAddressValidationLength;

    private Integer denominationLength;
    private String denominationValidationTypeValue;
    private String denominationValidationRegexValue;
    private String denominationValidationExcludedCharacter;

    private boolean checkTaxonomyCodeEnabled;

    private boolean enableTaxIdExternalValidation;

    private boolean skipCheckTaxIdInBlackList;

    private Instant physicalAddressLookupStartDate;

    private String latestNotificationVersion;

    private String documentNumberOfPagesTagKey;

    @PostConstruct
    public void init(){
        log.info("CONFIGURATION {}",this);
    }

    @Data
    public static class Topics {
        private String newNotifications;
        private String paymentEvents;
        private String asseverationEvents;
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
    public static class TaxonomyCodeDao {
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
    public static class PaNotificationLimitDao {
        private String tableName;
    }

    @Data
    public static class NotificationRefusedVerificationDao {
        private String tableName;
    }

}
