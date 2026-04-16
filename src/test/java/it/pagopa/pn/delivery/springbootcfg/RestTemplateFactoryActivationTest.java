package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.commons.pnclients.RestTemplateRetryable;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class RestTemplateFactoryActivationTest {
    @Test
    void returnsRetryableRestTemplateForValidConfiguration() {
        RestTemplateFactoryActivation activation = new RestTemplateFactoryActivation();

        RestTemplate result = activation.customizableRestTemplate(3,1000,2000);

        assertNotNull(result);
        assertInstanceOf(RestTemplateRetryable.class, result);
    }

    @Test
    void createsANewTemplateInstanceForEachInvocation() {
        RestTemplateFactoryActivation activation = new RestTemplateFactoryActivation();

        RestTemplate first = activation.customizableRestTemplate(1,500,500);
        RestTemplate second = activation.customizableRestTemplate(1,500,500);

        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second);
    }
}