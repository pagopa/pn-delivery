package it.pagopa.pn.delivery.svc.validation.validators.authorization;

import it.pagopa.pn.delivery.config.InformalNotificationSendPaParameterConsumer;
import it.pagopa.pn.delivery.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.DEFAULT_CX_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SendInformalNotificationActiveValidatorTest {

    @Test
    void shouldReturnSuccessWhenSenderIsEnabledForInformalNotifications() {
        InformalNotificationSendPaParameterConsumer parameterConsumer = mock(InformalNotificationSendPaParameterConsumer.class);
        when(parameterConsumer.isSenderActiveForInformalNotification(DEFAULT_CX_ID)).thenReturn(true);

        SendInformalNotificationActiveValidator validator = new SendInformalNotificationActiveValidator(parameterConsumer);

        assertDoesNotThrow(() -> validator.validate(DEFAULT_CX_ID));
    }

    @Test
    void shouldReturnErrorWhenSenderIsDisabledForInformalNotifications() {
        InformalNotificationSendPaParameterConsumer parameterConsumer = mock(InformalNotificationSendPaParameterConsumer.class);
        when(parameterConsumer.isSenderActiveForInformalNotification(DEFAULT_CX_ID)).thenReturn(false);

        SendInformalNotificationActiveValidator validator = new SendInformalNotificationActiveValidator(parameterConsumer);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(DEFAULT_CX_ID));

        assertEquals(1, exception.getProblem().getErrors().size());
        assertEquals("PN_DELIVERY_SEND_IS_DISABLED", exception.getProblem().getErrors().get(0).getCode());
        assertEquals("Piattaforma Notifiche non è abilitata alla comunicazione di notifiche bonarie",
                exception.getProblem().getErrors().get(0).getDetail());
    }
}