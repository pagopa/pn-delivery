package it.pagopa.pn.delivery.svc.validation.pipeline;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.config.InformalNotificationSendPaParameterConsumer;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.svc.validation.context.NotificaLegaleContext;
import it.pagopa.pn.delivery.svc.validation.context.ValidationContext;
import it.pagopa.pn.delivery.svc.validation.validators.authorization.SendInformalNotificationActiveValidator;
import it.pagopa.pn.delivery.svc.validation.validators.authorization.SenderTaxIdCongruenceValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ValidationPipelineFactory<C extends ValidationContext> {

    private final PnDeliveryConfigs cfg;
    private final PnExternalRegistriesClientImpl pnExternalRegistriesClient;
    private final InformalNotificationSendPaParameterConsumer parameterConsumer;

    //Validators
    private final SenderTaxIdCongruenceValidator senderTaxIdCongruenceValidator;
    private final RecipientTaxIdSyntaxValidator recipientTaxIdSyntaxValidator;

    private ValidationPipeline buildInformalPipeline() {
        return new ValidationPipelineBuilder<NotificaLegaleContext>()
                .authorization(senderTaxIdCongruenceValidator)
                .authorization(new SendInformalNotificationActiveValidator(parameterConsumer))
                .formal(new AdditionalLanguageFormalValidator())
                .formal(new CampaignMessageLanguageValidator(cfg.isInformalNotificationCheckCampaignLangActive()))
                .formal(new DenominationAndAtValidator(cfg.getDenominationLength(), cfg.getDenominationValidationTypeValue(), cfg.getDenominationValidationRegexValue(), cfg.getDenominationValidationExcludedCharacter()))
                .formal(new GroupValidator())
                .formal(new MaxAttachmentsSizeValidator(cfg.getInformalNotificationMaxAttachments()))
                .formal(new MaxPaymentsSizeValidator(cfg.getInformalNotificationMaxPayments()))
                .formal(new MaxRecipientsSizeValidator(cfg.getInformalNotificationMaxRecipients()))
                .formal(new PaymentAttachmentValidator())
                .formal(new PgTaxIdValidator())
                .formal(new PhysicalAddressValidator(cfg.isPhysicalAddressValidation(), cfg.getPhysicalAddressValidationLength(), cfg.getPhysicalAddressValidationPattern()))
                .formal(new ProvinceRequiredValidator())
                .formal(recipientTaxIdSyntaxValidator)
                .formal(new UniqueAttachmentsValidator())
                .build();
    }

    public static <C extends ValidationContext> ValidationPipelineBuilder<C> builder() {
        return new ValidationPipelineBuilder<>();
    }
}
