package it.pagopa.pn.delivery.svc.validation.validators.authorization;

import it.pagopa.pn.delivery.config.InformalNotificationSendPaParameterConsumer;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import org.junit.jupiter.api.Test;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.DEFAULT_CX_ID;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSingleError;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.legalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SendInformalNotificationActiveValidatorTest {

    @Test
    void shouldReturnSuccessWhenSenderIsEnabledForInformalNotifications() {
        InformalNotificationSendPaParameterConsumer parameterConsumer = mock(InformalNotificationSendPaParameterConsumer.class);
        when(parameterConsumer.isSenderActiveForInformalNotification(DEFAULT_CX_ID)).thenReturn(true);

        SendInformalNotificationActiveValidator validator = new SendInformalNotificationActiveValidator(parameterConsumer);
        ValidationResult result = validator.validate(legalContext(notification(java.util.List.of(), java.util.List.of())));

        assertSuccess(result);
    }

    @Test
    void shouldReturnErrorWhenSenderIsDisabledForInformalNotifications() {
        InformalNotificationSendPaParameterConsumer parameterConsumer = mock(InformalNotificationSendPaParameterConsumer.class);
        when(parameterConsumer.isSenderActiveForInformalNotification(DEFAULT_CX_ID)).thenReturn(false);

        SendInformalNotificationActiveValidator validator = new SendInformalNotificationActiveValidator(parameterConsumer);
        ValidationResult result = validator.validate(legalContext(new InternalNotification()));

        assertSingleError(result, ErrorCodes.ERROR_CODE_SEND_IS_DISABLED.getValue(), "non é abilitata alla comunicazione di notifiche bonarie");
    }
}
