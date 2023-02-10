package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.commons.configs.TaxIdInWhiteListParameterConsumer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaxIdInWhiteListActivation extends TaxIdInWhiteListParameterConsumer {
    public TaxIdInWhiteListActivation(ParameterConsumer parameterConsumer) {
        super(parameterConsumer);
    }
}
