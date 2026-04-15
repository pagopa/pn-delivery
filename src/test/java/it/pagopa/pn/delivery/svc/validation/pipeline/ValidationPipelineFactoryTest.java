package it.pagopa.pn.delivery.svc.validation.pipeline;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.config.InformalNotificationSendPaParameterConsumer;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.svc.validation.validators.AuthorizationValidator;
import it.pagopa.pn.delivery.svc.validation.validators.BusinessValidator;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import it.pagopa.pn.delivery.svc.validation.validators.authorization.SendInformalNotificationActiveValidator;
import it.pagopa.pn.delivery.svc.validation.validators.authorization.SenderTaxIdCongruenceValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.AdditionalLanguageFormalValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.CampaignMessageLanguageValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.DenominationAndAtValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.GroupValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.MaxAttachmentsSizeValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.MaxPaymentsSizeValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.MaxRecipientsSizeValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.PaymentAttachmentValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.PgTaxIdValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.PhysicalAddressValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.ProvinceRequiredValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.RecipientTaxIdSyntaxValidator;
import it.pagopa.pn.delivery.svc.validation.validators.formal.UniqueAttachmentsValidator;
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
    private PnExternalRegistriesClientImpl pnExternalRegistriesClient;
    private InformalNotificationSendPaParameterConsumer parameterConsumer;
    private SenderTaxIdCongruenceValidator senderTaxIdCongruenceValidator;
    private RecipientTaxIdSyntaxValidator recipientTaxIdSyntaxValidator;

    private ValidationPipelineFactory<?> factory;

    @BeforeEach
    void setUp() {
        cfg = mock(PnDeliveryConfigs.class);
        pnExternalRegistriesClient = mock(PnExternalRegistriesClientImpl.class);
        parameterConsumer = mock(InformalNotificationSendPaParameterConsumer.class);
        senderTaxIdCongruenceValidator = mock(SenderTaxIdCongruenceValidator.class);
        recipientTaxIdSyntaxValidator = mock(RecipientTaxIdSyntaxValidator.class);

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

        factory = new ValidationPipelineFactory<>(
                cfg,
                pnExternalRegistriesClient,
                parameterConsumer,
                senderTaxIdCongruenceValidator,
                recipientTaxIdSyntaxValidator
        );
    }

    @Test
    void buildInformalPipelineShouldComposeValidatorsInExpectedOrder() {
        ValidationPipeline<?> pipeline = buildInformalPipeline();

        List<AuthorizationValidator<?>> authorizationValidators = getValidators(pipeline, "authorizationValidators");
        List<FormalValidator<?>> formalValidators = getValidators(pipeline, "formalValidators");
        List<BusinessValidator<?>> businessValidators = getValidators(pipeline, "businessValidators");

        assertThat(authorizationValidators).hasSize(2);
        assertThat(authorizationValidators.get(0)).isSameAs(senderTaxIdCongruenceValidator);
        assertThat(authorizationValidators.get(1)).isInstanceOf(SendInformalNotificationActiveValidator.class);

        assertThat(formalValidators).hasSize(13);
        assertThat(formalValidators.get(0)).isInstanceOf(AdditionalLanguageFormalValidator.class);
        assertThat(formalValidators.get(1)).isInstanceOf(CampaignMessageLanguageValidator.class);
        assertThat(formalValidators.get(2)).isInstanceOf(DenominationAndAtValidator.class);
        assertThat(formalValidators.get(3)).isInstanceOf(GroupValidator.class);
        assertThat(formalValidators.get(4)).isInstanceOf(MaxAttachmentsSizeValidator.class);
        assertThat(formalValidators.get(5)).isInstanceOf(MaxPaymentsSizeValidator.class);
        assertThat(formalValidators.get(6)).isInstanceOf(MaxRecipientsSizeValidator.class);
        assertThat(formalValidators.get(7)).isInstanceOf(PaymentAttachmentValidator.class);
        assertThat(formalValidators.get(8)).isInstanceOf(PgTaxIdValidator.class);
        assertThat(formalValidators.get(9)).isInstanceOf(PhysicalAddressValidator.class);
        assertThat(formalValidators.get(10)).isInstanceOf(ProvinceRequiredValidator.class);
        assertThat(formalValidators.get(11)).isSameAs(recipientTaxIdSyntaxValidator);
        assertThat(formalValidators.get(12)).isInstanceOf(UniqueAttachmentsValidator.class);

        assertThat(businessValidators).isEmpty();
    }

    @Test
    void buildInformalPipelineShouldPropagateDependenciesAndConfigurationToCreatedValidators() {
        ValidationPipeline<?> pipeline = buildInformalPipeline();
        List<AuthorizationValidator<?>> authorizationValidators = getValidators(pipeline, "authorizationValidators");
        List<FormalValidator<?>> formalValidators = getValidators(pipeline, "formalValidators");

        SendInformalNotificationActiveValidator sendInformalNotificationActiveValidator =
                (SendInformalNotificationActiveValidator) authorizationValidators.get(1);
        CampaignMessageLanguageValidator campaignMessageLanguageValidator =
                (CampaignMessageLanguageValidator) formalValidators.get(1);
        DenominationAndAtValidator denominationAndAtValidator =
                (DenominationAndAtValidator) formalValidators.get(2);
        MaxAttachmentsSizeValidator maxAttachmentsSizeValidator =
                (MaxAttachmentsSizeValidator) formalValidators.get(4);
        MaxPaymentsSizeValidator maxPaymentsSizeValidator =
                (MaxPaymentsSizeValidator) formalValidators.get(5);
        MaxRecipientsSizeValidator maxRecipientsSizeValidator =
                (MaxRecipientsSizeValidator) formalValidators.get(6);
        PhysicalAddressValidator physicalAddressValidator =
                (PhysicalAddressValidator) formalValidators.get(9);

        assertThat(ReflectionTestUtils.getField(sendInformalNotificationActiveValidator, "parameterConsumer"))
                .isSameAs(parameterConsumer);

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
        assertThat(ReflectionTestUtils.getField(maxPaymentsSizeValidator, "getMaxPayments"))
                .isEqualTo(MAX_PAYMENT_NUMBER);
        assertThat(ReflectionTestUtils.getField(maxRecipientsSizeValidator, "maxRecipients"))
                .isEqualTo(MAX_RECIPIENTS_COUNT);

        assertThat(ReflectionTestUtils.getField(physicalAddressValidator, "physicalValidationActivated"))
                .isEqualTo(PHYSICAL_ADDRESS_VALIDATION_ACTIVE);
        assertThat(ReflectionTestUtils.getField(physicalAddressValidator, "length"))
                .isEqualTo(PHYSICAL_ADDRESS_VALIDATION_LENGTH);
        assertThat(ReflectionTestUtils.getField(physicalAddressValidator, "pattern"))
                .isEqualTo(PHYSICAL_ADDRESS_VALIDATION_PATTERN);
    }

    @Test
    void builderShouldReturnAnEmptyPipelineBuilder() {
        ValidationPipelineBuilder<?> builder = ValidationPipelineFactory.builder();
        ValidationPipeline<?> pipeline = builder.build();

        assertThat(getValidators(pipeline, "authorizationValidators")).isEmpty();
        assertThat(getValidators(pipeline, "formalValidators")).isEmpty();
        assertThat(getValidators(pipeline, "businessValidators")).isEmpty();
    }

    private ValidationPipeline<?> buildInformalPipeline() {
        return (ValidationPipeline<?>) ReflectionTestUtils.invokeMethod(factory, "buildInformalPipeline");
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getValidators(ValidationPipeline<?> pipeline, String fieldName) {
        return (List<T>) ReflectionTestUtils.getField(pipeline, fieldName);
    }
}
