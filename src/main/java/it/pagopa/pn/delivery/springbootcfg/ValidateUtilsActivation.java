package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.commons.configs.TaxIdInWhiteListParameterConsumer;
import it.pagopa.pn.commons.utils.ValidateUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidateUtilsActivation extends ValidateUtils {
    public ValidateUtilsActivation(TaxIdInWhiteListParameterConsumer taxIdInWhiteListParameterConsumer) {
        super(taxIdInWhiteListParameterConsumer);
    }
}
