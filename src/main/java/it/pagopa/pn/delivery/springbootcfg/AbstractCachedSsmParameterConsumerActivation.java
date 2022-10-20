package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.commons.abstractions.impl.AbstractCachedSsmParameterConsumer;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
public class AbstractCachedSsmParameterConsumerActivation extends AbstractCachedSsmParameterConsumer {
    public AbstractCachedSsmParameterConsumerActivation(SsmClient ssmClient) {
        super(ssmClient);
    }
}
