package it.pagopa.pn.delivery;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery")
@Data
public class PnDeliveryConfigs {

    private Topics topics;

    @Data
    public static class Topics {

        private String newNotifications;
        private String notificationAcknowledgement;

    }
}
