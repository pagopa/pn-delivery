package it.pagopa.pn.delivery.svc.validation.pipeline;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.validation.context.InformalNotificationContext;
import it.pagopa.pn.delivery.svc.validation.validators.authorization.SendInformalNotificationActiveValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ValidationPipelineFactory {

    private final PnDeliveryConfigs cfg;

    //Validators
    private final RecipientTaxIdSyntaxValidator recipientTaxIdSyntaxValidator;
    private final SendInformalNotificationActiveValidator sendInformalNotificationActiveValidator;
    private final AdditionalLanguageFormalValidator additionalLanguageFormalValidator;
    private final PgTaxIdValidator pgTaxIdValidator;
    private final GroupValidator groupValidator;
    private final PaymentAttachmentValidator paymentAttachmentValidator;
    private final ProvinceRequiredValidator provinceRequiredValidator;
    private final UniqueAttachmentsValidator uniqueAttachmentsValidator;
    private final PhysicalAddressValidator physicalAddressValidator;

    @Bean
    public ValidationPipeline<InformalNotificationContext> informalPipeline() {
        return new ValidationPipelineBuilder<InformalNotificationContext>()
                .authorization(sendInformalNotificationActiveValidator)
                .formal(new MaxAttachmentsSizeValidator(cfg.getInformalNotificationMaxAttachments()))
                .formal(new MaxRecipientsSizeValidator(cfg.getInformalNotificationMaxRecipients()))
                .formal(additionalLanguageFormalValidator)
                .formal(new CampaignMessageLanguageValidator(cfg.isInformalNotificationCheckCampaignLangActive()))
                .formal(groupValidator)
                .formal(pgTaxIdValidator)
                .formal(recipientTaxIdSyntaxValidator)
                .formal(new MaxPaymentsSizeValidator(cfg.getInformalNotificationMaxPayments()))
                .formal(paymentAttachmentValidator)
                .formal(physicalAddressValidator)
                .formal(provinceRequiredValidator)
                .formal(new DenominationAndAtValidator(cfg.getDenominationLength(), cfg.getDenominationValidationTypeValue(), cfg.getDenominationValidationRegexValue(), cfg.getDenominationValidationExcludedCharacter()))
                .formal(uniqueAttachmentsValidator)
                .build();
    }

}
