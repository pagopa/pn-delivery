package it.pagopa.pn.delivery.utils.io;

import it.pagopa.tech.lollipop.consumer.assertion.AssertionServiceFactory;
import it.pagopa.tech.lollipop.consumer.assertion.client.simple.AssertionSimpleClientProvider;
import it.pagopa.tech.lollipop.consumer.assertion.impl.AssertionServiceFactoryImpl;
import it.pagopa.tech.lollipop.consumer.assertion.storage.SimpleAssertionStorageProvider;
import it.pagopa.tech.lollipop.consumer.exception.LollipopVerifierException;
import it.pagopa.tech.lollipop.consumer.http_verifier.HttpMessageVerifierFactory;
import it.pagopa.tech.lollipop.consumer.http_verifier.visma.VismaHttpMessageVerifierFactory;
import it.pagopa.tech.lollipop.consumer.idp.IdpCertProviderFactory;
import it.pagopa.tech.lollipop.consumer.idp.client.simple.IdpCertSimpleClientConfig;
import it.pagopa.tech.lollipop.consumer.idp.client.simple.IdpCertSimpleClientProvider;
import it.pagopa.tech.lollipop.consumer.idp.client.simple.storage.SimpleIdpCertStorageProvider;
import it.pagopa.tech.lollipop.consumer.idp.impl.IdpCertProviderFactoryImpl;
import it.pagopa.tech.lollipop.consumer.idp.storage.IdpCertStorageConfig;
import it.pagopa.tech.lollipop.consumer.logger.LollipopLoggerServiceFactory;
import it.pagopa.tech.lollipop.consumer.logger.impl.LollipopLogbackLoggerServiceFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {SpringLollipopConsumerRequestConfig.class, SpringAssertionStorageConfig.class,
        SpringAssertionSimpleClientConfig.class, SpringIdpCertSimpleClientConfig.class, SpringIdpCertStorageConfig.class, SampleLollipopConsumerConfig.class})
public class SampleServicesConfig {

    @Bean
    public LollipopLoggerServiceFactory lollipopLoggerServiceFactory() {
        return new LollipopLogbackLoggerServiceFactory();
    }

    @Bean
    public HttpMessageVerifierFactory httpMessageVerifierFactory(
            SpringLollipopConsumerRequestConfig springLollipopConsumerRequestConfig) throws LollipopVerifierException {
        return new VismaHttpMessageVerifierFactory("UTF-8", springLollipopConsumerRequestConfig);
    }

    @Bean
    public IdpCertProviderFactory idpCertProviderFactory(IdpCertSimpleClientConfig simpleClientConfig,
                                                         IdpCertStorageConfig idpCertStorageConfig) {
        return new IdpCertProviderFactoryImpl(
                new IdpCertSimpleClientProvider(simpleClientConfig,new SimpleIdpCertStorageProvider(), idpCertStorageConfig));
    }

    @Bean
    public AssertionServiceFactory assertionServiceFactory(
            SpringAssertionSimpleClientConfig assertionSimpleClientConfig,
            SpringAssertionStorageConfig assertionStorageConfig) {
        return new AssertionServiceFactoryImpl(
                new SimpleAssertionStorageProvider(),
                new AssertionSimpleClientProvider(assertionSimpleClientConfig),
                assertionStorageConfig);
    }

}
