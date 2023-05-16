/* (C)2023 */
package it.pagopa.pn.delivery.utils.io;

import it.pagopa.tech.lollipop.consumer.assertion.AssertionServiceFactory;
import it.pagopa.tech.lollipop.consumer.command.LollipopConsumerCommandBuilder;
import it.pagopa.tech.lollipop.consumer.command.impl.LollipopConsumerCommandBuilderImpl;
import it.pagopa.tech.lollipop.consumer.helper.LollipopConsumerFactoryHelper;
import it.pagopa.tech.lollipop.consumer.http_verifier.HttpMessageVerifierFactory;
import it.pagopa.tech.lollipop.consumer.idp.IdpCertProviderFactory;
import it.pagopa.tech.lollipop.consumer.logger.LollipopLoggerServiceFactory;
import it.pagopa.tech.lollipop.consumer.service.LollipopConsumerRequestValidationService;
import it.pagopa.tech.lollipop.consumer.service.impl.LollipopConsumerRequestValidationServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Instance of Spring configuration of the core elements, the implementations of the related
 * services are delegated to external configurations
 */
@Configuration
public class HttpVerifierConfiguration {

    @Bean
    public LollipopConsumerFactoryHelper lollipopConsumerFactoryHelper(
            LollipopLoggerServiceFactory lollipopLoggerServiceFactory,
            HttpMessageVerifierFactory httpMessageVerifierFactory,
            IdpCertProviderFactory idpCertProviderFactory,
            AssertionServiceFactory assertionServiceFactory,
            LollipopConsumerRequestValidationService lollipopConsumerRequestValidationService,
            SpringLollipopConsumerRequestConfig springLollipopConsumerRequestConfig) {
        return new LollipopConsumerFactoryHelper(
                lollipopLoggerServiceFactory,
                httpMessageVerifierFactory,
                idpCertProviderFactory,
                assertionServiceFactory,
                lollipopConsumerRequestValidationService,
                springLollipopConsumerRequestConfig);
    }

    @Bean
    public LollipopConsumerRequestValidationService getLollipopConsumerRequestValidationService(
            SpringLollipopConsumerRequestConfig springLollipopConsumerRequestConfig) {
        return new LollipopConsumerRequestValidationServiceImpl(
                springLollipopConsumerRequestConfig);
    }

    @Bean
    public LollipopConsumerCommandBuilder lollipopConsumerCommandBuilder(
            LollipopConsumerFactoryHelper lollipopConsumerFactoryHelper) {
        return new LollipopConsumerCommandBuilderImpl(lollipopConsumerFactoryHelper);
    }

    @Bean
    public HttpVerifierHandlerInterceptor httpVerifierHandlerInterceptor(
            LollipopConsumerCommandBuilder lollipopConsumerCommandBuilder) {
        return new HttpVerifierHandlerInterceptor(lollipopConsumerCommandBuilder);
    }
}
