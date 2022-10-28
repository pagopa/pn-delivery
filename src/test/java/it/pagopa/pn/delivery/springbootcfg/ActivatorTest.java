package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.commons.abstractions.impl.AbstractCachedSsmParameterConsumer;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.services.ssm.SsmClient;

class ActivatorTest {

    @Mock
    private AwsConfigs awsConfigs;
    @Mock
    private AbstractCachedSsmParameterConsumer abstractCachedSsmParameterConsumer;
    @Mock
    private SsmClient ssmClient;
    @Mock
    private ExceptionHelper exceptionHelper;

    @Test
    void activatorTest(){
        Assertions.assertDoesNotThrow( ()  -> {
            new AbstractCachedSsmParameterConsumerActivation(ssmClient);
            new AwsConfigsActivation();
            new AwsServicesClientsConfigActivation(awsConfigs);
            new ClockConfigActivation();
            new IsMVPParameterConsumerActivation(abstractCachedSsmParameterConsumer);
            new PnResponseEntityExceptionHandlerActivation(exceptionHelper);
        });
    }
}
