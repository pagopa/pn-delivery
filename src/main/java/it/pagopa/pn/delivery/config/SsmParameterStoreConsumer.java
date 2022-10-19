package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.impl.AbstractSsmParameterConsumer;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ssm.SsmClient;

@Component
public class SsmParameterStoreConsumer extends AbstractSsmParameterConsumer implements ParameterStoreConsumer {
    public SsmParameterStoreConsumer(SsmClient ssmClient) {
        super(ssmClient);
    }
}
