package it.pagopa.pn.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

import java.time.Duration;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery")
@Data
public class PnDeliveryConfigs {

    private String externalChannelBaseUrl;

    private Topics topics;

    private Duration preloadUrlDuration;

    private Duration downloadUrlDuration;

    private boolean downloadWithPresignedUrl;

    private Integer numberOfPresignedRequest;

    @Data
    public static class Topics {

        private String newNotifications;
    }
}
