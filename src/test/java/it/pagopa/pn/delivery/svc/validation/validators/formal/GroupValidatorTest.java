package it.pagopa.pn.delivery.svc.validation.validators.formal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.legalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;

class GroupValidatorTest {

    @Test
    void shouldAlwaysReturnSuccess() {
        GroupValidator validator = new GroupValidator();
        assertSuccess(validator.validate(legalContext(notification(List.of(), List.of()))));
    }
}

