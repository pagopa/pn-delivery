package it.pagopa.pn.delivery.middleware;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties( prefix = "pn.topics")
@Data
public class ProducerConfigs {

    private String newnotifications;

}
