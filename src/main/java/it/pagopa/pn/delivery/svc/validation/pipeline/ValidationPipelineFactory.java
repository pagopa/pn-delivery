package it.pagopa.pn.delivery.svc.validation.pipeline;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.validation.context.InformalNotificationContext;
import it.pagopa.pn.delivery.svc.validation.context.ValidationContext;
import it.pagopa.pn.delivery.svc.validation.validators.authorization.SendInformalNotificationActiveValidator;
import it.pagopa.pn.delivery.svc.validation.validators.authorization.SenderTaxIdCongruenceValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ValidationPipelineFactory<C extends ValidationContext<?>> {

    private final PnDeliveryConfigs cfg;

    //Validators
    private final SenderTaxIdCongruenceValidator senderTaxIdCongruenceValidator;
    private final RecipientTaxIdSyntaxValidator recipientTaxIdSyntaxValidator;
    private final SendInformalNotificationActiveValidator sendInformalNotificationActiveValidator;
    private final AdditionalLanguageFormalValidator additionalLanguageFormalValidator;
    private final PgTaxIdValidator pgTaxIdValidator;
    private final GroupValidator groupValidator;
    private final PaymentAttachmentValidator paymentAttachmentValidator;
    private final ProvinceRequiredValidator provinceRequiredValidator;
    private final UniqueAttachmentsValidator uniqueAttachmentsValidator;

    @Bean
    public ValidationPipeline<?> informalPipeline() {
        return new ValidationPipelineBuilder<InformalNotificationContext>()
                .authorization(sendInformalNotificationActiveValidator)
                .authorization(senderTaxIdCongruenceValidator)
                .formal(new MaxAttachmentsSizeValidator(cfg.getInformalNotificationMaxAttachments()))
                .formal(new MaxRecipientsSizeValidator(cfg.getInformalNotificationMaxRecipients()))
                .formal(additionalLanguageFormalValidator)
                .formal(new CampaignMessageLanguageValidator(cfg.isInformalNotificationCheckCampaignLangActive()))
                .formal(groupValidator)
                .formal(pgTaxIdValidator)
                .formal(recipientTaxIdSyntaxValidator)
                .formal(new MaxPaymentsSizeValidator(cfg.getInformalNotificationMaxPayments()))
                .formal(paymentAttachmentValidator)
                .formal(new PhysicalAddressValidator(cfg.isPhysicalAddressValidation(), cfg.getPhysicalAddressValidationLength(), cfg.getPhysicalAddressValidationPattern()))
                .formal(provinceRequiredValidator)
                .formal(new DenominationAndAtValidator(cfg.getDenominationLength(), cfg.getDenominationValidationTypeValue(), cfg.getDenominationValidationRegexValue(), cfg.getDenominationValidationExcludedCharacter()))
                .formal(uniqueAttachmentsValidator)
                .build();
    }

    public static <C extends ValidationContext<?>> ValidationPipelineBuilder<C> builder() {
        return new ValidationPipelineBuilder<>();
    }
}
