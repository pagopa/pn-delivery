package it.pagopa.pn.delivery.svc.validation.pipeline;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.validation.validators.AuthorizationValidator;
import it.pagopa.pn.delivery.svc.validation.validators.BusinessValidator;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import it.pagopa.pn.delivery.svc.validation.validators.authorization.SenderTaxIdCongruenceValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidationPipelineFactoryTest {

    private static final Integer DENOMINATION_LENGTH = 44;
    private static final String DENOMINATION_VALIDATION_TYPE = "REGEX";
    private static final String DENOMINATION_VALIDATION_REGEX = "a-zA-Z0-9";
    private static final String DENOMINATION_VALIDATION_EXCLUDED_CHARACTER = "|";
    private static final Integer MAX_ATTACHMENTS_COUNT = 5;
    private static final Integer MAX_PAYMENT_NUMBER = 3;
    private static final Integer MAX_RECIPIENTS_COUNT = 2;
    private static final boolean CAMPAIGN_LANG_CHECK_ACTIVE = true;
    private static final boolean PHYSICAL_ADDRESS_VALIDATION_ACTIVE = true;
    private static final Integer PHYSICAL_ADDRESS_VALIDATION_LENGTH = 44;
    private static final String PHYSICAL_ADDRESS_VALIDATION_PATTERN = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ./ '-";

    private PnDeliveryConfigs cfg;
    private SenderTaxIdCongruenceValidator senderTaxIdCongruenceValidator;
    private RecipientTaxIdSyntaxValidator recipientTaxIdSyntaxValidator;
    private AdditionalLanguageFormalValidator additionalLanguageFormalValidator;
    private PgTaxIdValidator pgTaxIdValidator;
    private GroupValidator groupValidator;
    private PaymentAttachmentValidator paymentAttachmentValidator;
    private ProvinceRequiredValidator provinceRequiredValidator;
    private UniqueAttachmentsValidator uniqueAttachmentsValidator;
    private PhysicalAddressValidator physicalAddressValidator;

    private ValidationPipelineFactory factory;

    @BeforeEach
    void setUp() {
        cfg = mock(PnDeliveryConfigs.class);
        senderTaxIdCongruenceValidator = mock(SenderTaxIdCongruenceValidator.class);
        recipientTaxIdSyntaxValidator = mock(RecipientTaxIdSyntaxValidator.class);
        additionalLanguageFormalValidator = mock(AdditionalLanguageFormalValidator.class);
        pgTaxIdValidator = mock(PgTaxIdValidator.class);
        groupValidator = mock(GroupValidator.class);
        paymentAttachmentValidator = mock(PaymentAttachmentValidator.class);
        provinceRequiredValidator = mock(ProvinceRequiredValidator.class);
        uniqueAttachmentsValidator = mock(UniqueAttachmentsValidator.class);
        physicalAddressValidator = mock(PhysicalAddressValidator.class);

        when(cfg.getDenominationLength()).thenReturn(DENOMINATION_LENGTH);
        when(cfg.getDenominationValidationTypeValue()).thenReturn(DENOMINATION_VALIDATION_TYPE);
        when(cfg.getDenominationValidationRegexValue()).thenReturn(DENOMINATION_VALIDATION_REGEX);
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn(DENOMINATION_VALIDATION_EXCLUDED_CHARACTER);
        when(cfg.getInformalNotificationMaxAttachments()).thenReturn(MAX_ATTACHMENTS_COUNT);
        when(cfg.getInformalNotificationMaxPayments()).thenReturn(MAX_PAYMENT_NUMBER);
        when(cfg.getInformalNotificationMaxRecipients()).thenReturn(MAX_RECIPIENTS_COUNT);
        when(cfg.isInformalNotificationCheckCampaignLangActive()).thenReturn(CAMPAIGN_LANG_CHECK_ACTIVE);
        when(cfg.isPhysicalAddressValidation()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_ACTIVE);
        when(cfg.getPhysicalAddressValidationLength()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_LENGTH);
        when(cfg.getPhysicalAddressValidationPattern()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_PATTERN);

        factory = new ValidationPipelineFactory(
                cfg,
                senderTaxIdCongruenceValidator,
                recipientTaxIdSyntaxValidator,
                additionalLanguageFormalValidator,
                pgTaxIdValidator,
                groupValidator,
                paymentAttachmentValidator,
                provinceRequiredValidator,
                uniqueAttachmentsValidator,
                physicalAddressValidator
        );
    }

    @Test
    void buildInformalPipelineShouldComposeValidatorsInExpectedOrder() {
        ValidationPipeline<?> pipeline = buildInformalPipeline();

        List<AuthorizationValidator<?>> authorizationValidators = getValidators(pipeline, "authorizationValidators");
        List<FormalValidator<?>> formalValidators = getValidators(pipeline, "formalValidators");
        List<BusinessValidator<?>> businessValidators = getValidators(pipeline, "businessValidators");

        assertThat(authorizationValidators).hasSize(1);
        assertThat(authorizationValidators.get(0)).isSameAs(senderTaxIdCongruenceValidator);

        assertThat(formalValidators).hasSize(13);
        assertThat(formalValidators.get(0)).isInstanceOf(MaxAttachmentsSizeValidator.class);
        assertThat(formalValidators.get(1)).isInstanceOf(MaxRecipientsSizeValidator.class);
        assertThat(formalValidators.get(2)).isSameAs(additionalLanguageFormalValidator);
        assertThat(formalValidators.get(3)).isInstanceOf(CampaignMessageLanguageValidator.class);
        assertThat(formalValidators.get(4)).isSameAs(groupValidator);
        assertThat(formalValidators.get(5)).isSameAs(pgTaxIdValidator);
        assertThat(formalValidators.get(6)).isSameAs(recipientTaxIdSyntaxValidator);
        assertThat(formalValidators.get(7)).isInstanceOf(MaxPaymentsSizeValidator.class);
        assertThat(formalValidators.get(8)).isSameAs(paymentAttachmentValidator);
        assertThat(formalValidators.get(9)).isSameAs(physicalAddressValidator);
        assertThat(formalValidators.get(10)).isSameAs(provinceRequiredValidator);
        assertThat(formalValidators.get(11)).isInstanceOf(DenominationAndAtValidator.class);
        assertThat(formalValidators.get(12)).isSameAs(uniqueAttachmentsValidator);

        assertThat(businessValidators).isEmpty();
    }

    @Test
    void buildInformalPipelineShouldPropagateDependenciesAndConfigurationToCreatedValidators() {
        ValidationPipeline<?> pipeline = buildInformalPipeline();

        List<AuthorizationValidator<?>> authorizationValidators = getValidators(pipeline, "authorizationValidators");
        List<FormalValidator<?>> formalValidators = getValidators(pipeline, "formalValidators");

        assertThat(authorizationValidators)
                .hasSize(1)
                .first()
                .isInstanceOf(SenderTaxIdCongruenceValidator.class);

        assertThat(formalValidators.get(2)).isSameAs(additionalLanguageFormalValidator);
        assertThat(formalValidators.get(4)).isSameAs(groupValidator);
        assertThat(formalValidators.get(5)).isSameAs(pgTaxIdValidator);
        assertThat(formalValidators.get(6)).isSameAs(recipientTaxIdSyntaxValidator);
        assertThat(formalValidators.get(8)).isSameAs(paymentAttachmentValidator);
        assertThat(formalValidators.get(9)).isSameAs(physicalAddressValidator);
        assertThat(formalValidators.get(10)).isSameAs(provinceRequiredValidator);
        assertThat(formalValidators.get(12)).isSameAs(uniqueAttachmentsValidator);

        CampaignMessageLanguageValidator campaignMessageLanguageValidator =
                (CampaignMessageLanguageValidator) formalValidators.get(3);
        DenominationAndAtValidator denominationAndAtValidator =
                (DenominationAndAtValidator) formalValidators.get(11);
        MaxAttachmentsSizeValidator maxAttachmentsSizeValidator =
                (MaxAttachmentsSizeValidator) formalValidators.get(0);
        MaxPaymentsSizeValidator maxPaymentsSizeValidator =
                (MaxPaymentsSizeValidator) formalValidators.get(7);
        MaxRecipientsSizeValidator maxRecipientsSizeValidator =
                (MaxRecipientsSizeValidator) formalValidators.get(1);


        assertThat(ReflectionTestUtils.getField(campaignMessageLanguageValidator, "isInformalNotificationCheckCampaignLangActive"))
                .isEqualTo(CAMPAIGN_LANG_CHECK_ACTIVE);

        assertThat(ReflectionTestUtils.getField(denominationAndAtValidator, "denominationLength"))
                .isEqualTo(DENOMINATION_LENGTH);
        assertThat(ReflectionTestUtils.getField(denominationAndAtValidator, "denominationValidationTypeValue"))
                .isEqualTo(DENOMINATION_VALIDATION_TYPE);
        assertThat(ReflectionTestUtils.getField(denominationAndAtValidator, "denominationValidationRegexValue"))
                .isEqualTo(DENOMINATION_VALIDATION_REGEX);
        assertThat(ReflectionTestUtils.getField(denominationAndAtValidator, "denominationValidationExcludedCharacter"))
                .isEqualTo(DENOMINATION_VALIDATION_EXCLUDED_CHARACTER);

        assertThat(ReflectionTestUtils.getField(maxAttachmentsSizeValidator, "maxAttachments"))
                .isEqualTo(MAX_ATTACHMENTS_COUNT);
        assertThat(ReflectionTestUtils.getField(maxPaymentsSizeValidator, "maxPayments"))
                .isEqualTo(MAX_PAYMENT_NUMBER);
        assertThat(ReflectionTestUtils.getField(maxRecipientsSizeValidator, "maxRecipients"))
                .isEqualTo(MAX_RECIPIENTS_COUNT);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getValidators(ValidationPipeline<?> pipeline, String fieldName) {
        return (List<T>) ReflectionTestUtils.getField(pipeline, fieldName);
    }

    private ValidationPipeline<?> buildInformalPipeline() {
        return factory.informalPipeline();
    }
}
