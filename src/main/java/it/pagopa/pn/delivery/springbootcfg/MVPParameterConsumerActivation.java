package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.commons.abstractions.impl.AbstractCachedSsmParameterConsumer;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MVPParameterConsumerActivation extends MVPParameterConsumer {
    public MVPParameterConsumerActivation(AbstractCachedSsmParameterConsumer abstractCachedSsmParameterConsumer) {
        super(abstractCachedSsmParameterConsumer);
    }
}
