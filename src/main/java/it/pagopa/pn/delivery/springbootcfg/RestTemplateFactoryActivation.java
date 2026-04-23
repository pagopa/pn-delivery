package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.commons.pnclients.RestTemplateFactory;
import it.pagopa.pn.commons.pnclients.RestTemplateRetryable;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateFactoryActivation extends RestTemplateFactory {
    public RestTemplate customizableRestTemplate(int retryMaxAttempts, int connectionTimeout, int readTimeout) {
        RestTemplate template = new RestTemplateRetryable(retryMaxAttempts + 1);
        this.configureRestTemplate(connectionTimeout, readTimeout, template);
        return template;
    }
}
