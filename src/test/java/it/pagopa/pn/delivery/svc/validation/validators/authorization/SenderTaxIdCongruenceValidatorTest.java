package it.pagopa.pn.delivery.svc.validation.validators.authorization;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaInfo;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import org.junit.jupiter.api.Test;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_PA_NOT_FOUND;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.DEFAULT_CX_ID;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.DEFAULT_SENDER_TAX_ID;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSingleError;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.legalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SenderTaxIdCongruenceValidatorTest {

    @Test
    void shouldReturnSuccessWhenFeatureIsDisabled() {
        PnDeliveryConfigs cfg = mock(PnDeliveryConfigs.class);
        PnExternalRegistriesClientImpl client = mock(PnExternalRegistriesClientImpl.class);
        when(cfg.isEnableSenderTaxIdCongruence()).thenReturn(false);

        SenderTaxIdCongruenceValidator validator = new SenderTaxIdCongruenceValidator(cfg, client);
        ValidationResult result = validator.validate(legalContext(notification(java.util.List.of(), java.util.List.of())));

        assertSuccess(result);
    }

    @Test
    void shouldReturnSuccessWhenSenderTaxIdMatchesExternalRegistries() {
        PnDeliveryConfigs cfg = mock(PnDeliveryConfigs.class);
        PnExternalRegistriesClientImpl client = mock(PnExternalRegistriesClientImpl.class);
        when(cfg.isEnableSenderTaxIdCongruence()).thenReturn(true);
        PaInfo paInfo = new PaInfo();
        paInfo.setTaxId(DEFAULT_SENDER_TAX_ID);
        when(client.getOnePa(DEFAULT_CX_ID)).thenReturn(paInfo);

        SenderTaxIdCongruenceValidator validator = new SenderTaxIdCongruenceValidator(cfg, client);
        ValidationResult result = validator.validate(legalContext(notification(java.util.List.of(), java.util.List.of())));

        assertSuccess(result);
    }

    @Test
    void shouldReturnPaNotFoundWhenExternalRegistriesDoNotProvideTaxId() {
        PnDeliveryConfigs cfg = mock(PnDeliveryConfigs.class);
        PnExternalRegistriesClientImpl client = mock(PnExternalRegistriesClientImpl.class);
        when(cfg.isEnableSenderTaxIdCongruence()).thenReturn(true);
        when(client.getOnePa(DEFAULT_CX_ID)).thenReturn(new PaInfo());

        SenderTaxIdCongruenceValidator validator = new SenderTaxIdCongruenceValidator(cfg, client);
        NotificationContext context = legalContext(new InternalNotification());
        PnInternalException exception = assertThrows(PnInternalException.class,
                () -> validator.validate(context));

        assertThat(exception.getProblem().getErrors()).hasSize(1);
        assertThat(exception.getProblem().getErrors().get(0).getCode()).isEqualTo(ERROR_CODE_DELIVERY_PA_NOT_FOUND);
    }

    @Test
    void shouldReturnErrorWhenSenderTaxIdDoesNotMatchExternalRegistries() {
        PnDeliveryConfigs cfg = mock(PnDeliveryConfigs.class);
        PnExternalRegistriesClientImpl client = mock(PnExternalRegistriesClientImpl.class);
        when(cfg.isEnableSenderTaxIdCongruence()).thenReturn(true);
        PaInfo paInfo = new PaInfo();
        paInfo.setTaxId("99999999999");
        when(client.getOnePa(DEFAULT_CX_ID)).thenReturn(paInfo);

        SenderTaxIdCongruenceValidator validator = new SenderTaxIdCongruenceValidator(cfg, client);
        ValidationResult result = validator.validate(legalContext(notification(java.util.List.of(), java.util.List.of())));

        assertSingleError(result, ErrorCodes.ERROR_CODE_INVALID_TAX_ID.getValue(), "risulta incongruente");
    }
}
