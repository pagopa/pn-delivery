package it.pagopa.pn.delivery.middleware.queue.consumer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "pn.delivery.event")
public class EventHandler {
    private Map<String, String> handler;
}
