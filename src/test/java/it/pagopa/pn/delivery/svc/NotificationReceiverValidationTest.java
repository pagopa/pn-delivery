package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.config.PhysicalAddressLookupParameterConsumer;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.generated.openapi.msclient.nationalregistries.v1.api.AgenziaEntrateApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.nationalregistries.v1.model.CheckTaxIdOK;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.utils.FeatureFlagUtils;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
class NotificationReceiverValidationTest {

    @Mock
    private PnDeliveryConfigs cfg;

    @Mock
    private ValidateUtils validateUtils;
    @Mock
    private AgenziaEntrateApi agenziaEntrateApi;

    public static final String SHA256_BODY = "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=";
    public static final String VERSION_TOKEN = "version_token";
    public static final String KEY = "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG"; // or also PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG
    public static final String PHYSICAL_ADDRESS_VALIDATION_PATTERN = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ./ '-";
    public static final Integer PHYSICAL_ADDRESS_VALIDATION_LENGTH = 44;

    private PhysicalAddressLookupParameterConsumer physicalAddressLookupParameter;

    private NotificationReceiverValidator validator;
    private MVPParameterConsumer mvpParameterConsumer;
    private FeatureFlagUtils featureFlagUtils;

    @BeforeEach
    void initializeValidator() {
        this.cfg = Mockito.mock(PnDeliveryConfigs.class);
        this.validateUtils = Mockito.mock(ValidateUtils.class);
        this.agenziaEntrateApi = Mockito.mock(AgenziaEntrateApi.class);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        mvpParameterConsumer = mock(MVPParameterConsumer.class);
        physicalAddressLookupParameter = Mockito.mock(PhysicalAddressLookupParameterConsumer.class);
        featureFlagUtils = Mockito.mock(FeatureFlagUtils.class);
        validator = new NotificationReceiverValidator(factory.getValidator(), mvpParameterConsumer, validateUtils, cfg, agenziaEntrateApi,physicalAddressLookupParameter, featureFlagUtils);
    }

    private void defaultMockConfigAndParameterForVas(){
        Mockito.when(physicalAddressLookupParameter.getActivePAsForPhysicalAddressLookup()).thenReturn(List.of("paId"));
        Mockito.when(featureFlagUtils.isPhysicalAddressLookupEnabled()).thenReturn(true);
    }
    @Test
    void invalidNotificationDeliveryModeNoPaFee() {
        NewNotificationRequestV25 newNotificationRequest = newNotificationWithoutPayments();
        newNotificationRequest.setNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);
        newNotificationRequest.setVat(22);
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;

        defaultMockConfigAndParameterForVas();

        errors = validator.checkNewNotificationRequestBeforeInsert(newNotificationRequest, newNotificationRequest.getSenderTaxId());

        assertThat(errors, hasItems(
                hasProperty("message", Matchers.containsString("paFee"))
        ));
    }

    @Test
    void invalidNotificationDeliveryModeNoVat() {
        NewNotificationRequestV25 newNotificationRequest = newNotificationWithoutPayments();
        newNotificationRequest.setNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);
        newNotificationRequest.setPaFee(100);
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        defaultMockConfigAndParameterForVas();

        errors = validator.checkNewNotificationRequestBeforeInsert(newNotificationRequest, newNotificationRequest.getSenderTaxId());

        assertThat(errors, hasItems(
                hasProperty("message", Matchers.containsString("vat"))
        ));
    }


    @Test
    @Disabled
    void checkThrowMechanism() {

    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));
    // WHEN
    Assertions.assertDoesNotThrow(() -> validator.checkNewNotificationBeforeInsertAndThrow(internalNotification));
  }

    @Test
    void checkOk() {
        InternalNotification n = validDocumentWithoutPayments();
        n.setNotificationFeePolicy(NotificationFeePolicy.FLAT_RATE);

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert(n);
        Assertions.assertTrue(errors.isEmpty()); // this is due to an error inside validator.checkNewNotificationBeforeInsert so go over
    }

    @Test
    void checkOk2() {
        InternalNotification n = validDocumentWithoutPayments();
        n.setNotificationFeePolicy(NotificationFeePolicy.FLAT_RATE);

        Assertions.assertDoesNotThrow(() -> validator.checkNewNotificationBeforeInsertAndThrow(n));
    }


    @Test
    void invalidRecipientPGTaxId() {
        // Given
        NewNotificationRequestV25 n = newNotificationPG();
        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        defaultMockConfigAndParameterForVas();

        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // Then
        assertConstraintViolationPresentByMessage(errors, "SEND accepts only numerical taxId for PG recipient 0");
        verifyNoInteractions(agenziaEntrateApi);
    }

    @Test
    void ValidRecipientPGTaxIdSkipAdE() {
        // Given
        NewNotificationRequestV25 n = newNotificationPG();
        n.getRecipients().get(0).setTaxId("76898480348");
        n.getRecipients().get(0).getPayments().get(0).getPagoPa().setCreditorTaxId("12345678901");
        n.setTaxonomyCode("123456A");
        n.senderTaxId("12345678901");
        n.physicalCommunicationType(NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        n.documents(Collections.singletonList(NotificationDocument.builder()
                .contentType("application/pdf")
                .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                        .build())
                .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build()));

        when(validateUtils.validate(anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(true);

        defaultMockConfigAndParameterForVas();

        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());
        // Then
        Assertions.assertTrue(errors.isEmpty());
        verifyNoInteractions(agenziaEntrateApi);
    }

    @Test
    void invalidRecipientPGTaxIdOnAdE() {
        // Given
        NewNotificationRequestV25 n = newNotificationPG();
        n.getRecipients().get(0).setTaxId("76898480348");
        n.getRecipients().get(0).getPayments().get(0).getPagoPa().setCreditorTaxId("12345678901");
        n.setTaxonomyCode("123456A");
        n.senderTaxId("12345678901");
        n.physicalCommunicationType(NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        n.documents(Collections.singletonList(NotificationDocument.builder()
                .contentType("application/pdf")
                .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                        .build())
                .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build()));

        when(validateUtils.validate(anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(true);

        when(cfg.isEnableTaxIdExternalValidation()).thenReturn(true);
        CheckTaxIdOK checkTaxIdOK = new CheckTaxIdOK();
        checkTaxIdOK.setIsValid(Boolean.FALSE);
        when(agenziaEntrateApi.checkTaxId(any())).thenReturn(checkTaxIdOK);
        defaultMockConfigAndParameterForVas();

        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // Then
        assertConstraintViolationPresentByMessage(errors, "Invalid taxId for recipient 0");
        verify(agenziaEntrateApi, times(1)).checkTaxId(any());

    }


    @Test
    void validRecipientPGTaxIdOnAdE() {
        // Given
        NewNotificationRequestV25 n = newNotificationPG();
        n.getRecipients().get(0).setTaxId("76898480348");
        n.getRecipients().get(0).getPayments().get(0).getPagoPa().setCreditorTaxId("12345678901");
        n.setTaxonomyCode("123456A");
        n.senderTaxId("12345678901");
        n.physicalCommunicationType(NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        n.documents(Collections.singletonList(NotificationDocument.builder()
                .contentType("application/pdf")
                .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                        .build())
                .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build()));

        when(validateUtils.validate(anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(true);

        when(cfg.isEnableTaxIdExternalValidation()).thenReturn(true);
        CheckTaxIdOK checkTaxIdOK = new CheckTaxIdOK();
        checkTaxIdOK.setIsValid(Boolean.TRUE);
        when(agenziaEntrateApi.checkTaxId(any())).thenReturn(checkTaxIdOK);

        defaultMockConfigAndParameterForVas();

        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        Assertions.assertTrue(errors.isEmpty());
        verify(agenziaEntrateApi, times(1)).checkTaxId(any());

    }

    @Test
    void invalidNotificationErrorOnAdE() {
        // Given
        NewNotificationRequestV25 n = newNotificationPG();
        n.getRecipients().get(0).setTaxId("76898480348");
        n.getRecipients().get(0).getPayments().get(0).getPagoPa().setCreditorTaxId("12345678901");
        n.setTaxonomyCode("123456A");
        n.senderTaxId("12345678901");
        n.physicalCommunicationType(NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        n.documents(Collections.singletonList(NotificationDocument.builder()
                .contentType("application/pdf")
                .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                        .build())
                .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build()));

        when(validateUtils.validate(anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(true);

        when(cfg.isEnableTaxIdExternalValidation()).thenReturn(true);
        when(agenziaEntrateApi.checkTaxId(any())).thenThrow(RestClientException.class);
        defaultMockConfigAndParameterForVas();

        Assertions.assertThrows(PnInternalException.class, () -> validator.checkNewNotificationRequestBeforeInsertAndThrow(n, n.getSenderTaxId()), "Error calling check taxId on AdE");
        verify(agenziaEntrateApi, times(1)).checkTaxId(any());
    }

    @Test
    void invalidRecipientPFTaxId() {
        // Given
        NewNotificationRequestV25 n = newNotification();
        defaultMockConfigAndParameterForVas();
        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // Then
        assertConstraintViolationPresentByMessage(errors, "Invalid taxId for recipient 0");
        verifyNoInteractions(agenziaEntrateApi);
    }

    @Test
    void ValidRecipientPFTaxIdSkipAdE() {
        // Given
        NewNotificationRequestV25 n = newNotification();
        n.getRecipients().get(0).setTaxId("PPPPLT80A01H501V");
        n.getRecipients().get(0).getPayments().get(0).getPagoPa().setCreditorTaxId("12345678901");
        n.setTaxonomyCode("123456A");
        n.senderTaxId("12345678901");
        n.physicalCommunicationType(NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        n.documents(Collections.singletonList(NotificationDocument.builder()
                .contentType("application/pdf")
                .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                        .build())
                .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build()));

        when(validateUtils.validate(anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(true);
        defaultMockConfigAndParameterForVas();

        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());
        // Then
        Assertions.assertTrue(errors.isEmpty());
        verifyNoInteractions(agenziaEntrateApi);

    }

    @Test
    void invalidRecipientPFTaxIdOnAdE() {
        // Given
        NewNotificationRequestV25 n = newNotificationPG();
        n.getRecipients().get(0).setTaxId("PPPPLT80A01H501V");
        n.getRecipients().get(0).getPayments().get(0).getPagoPa().setCreditorTaxId("12345678901");
        n.setTaxonomyCode("123456A");
        n.senderTaxId("12345678901");
        n.physicalCommunicationType(NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        n.documents(Collections.singletonList(NotificationDocument.builder()
                .contentType("application/pdf")
                .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                        .build())
                .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build()));

        when(validateUtils.validate(anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(true);

        when(cfg.isEnableTaxIdExternalValidation()).thenReturn(true);
        CheckTaxIdOK checkTaxIdOK = new CheckTaxIdOK();
        checkTaxIdOK.setIsValid(Boolean.FALSE);
        when(agenziaEntrateApi.checkTaxId(any())).thenReturn(checkTaxIdOK);
        defaultMockConfigAndParameterForVas();
        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // Then
        assertConstraintViolationPresentByMessage(errors, "Invalid taxId for recipient 0");
        verify(agenziaEntrateApi, times(1)).checkTaxId(any());

    }


    @Test
    void validRecipientPFTaxIdOnAdE() {
        // Given
        NewNotificationRequestV25 n = newNotification();
        n.getRecipients().get(0).setTaxId("PPPPLT80A01H501V");
        n.getRecipients().get(0).getPayments().get(0).getPagoPa().setCreditorTaxId("12345678901");
        n.setTaxonomyCode("123456A");
        n.senderTaxId("12345678901");
        n.physicalCommunicationType(NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        n.documents(Collections.singletonList(NotificationDocument.builder()
                .contentType("application/pdf")
                .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                        .build())
                .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build()));

        when(validateUtils.validate(anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(true);

        when(cfg.isEnableTaxIdExternalValidation()).thenReturn(true);
        CheckTaxIdOK checkTaxIdOK = new CheckTaxIdOK();
        checkTaxIdOK.setIsValid(Boolean.TRUE);
        when(agenziaEntrateApi.checkTaxId(any())).thenReturn(checkTaxIdOK);
        defaultMockConfigAndParameterForVas();

        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        Assertions.assertTrue(errors.isEmpty());
        verify(agenziaEntrateApi, times(1)).checkTaxId(any());

    }

    @Test
    @Disabled
    void invalidRecipient() {

    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));
    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(internalNotification);

        // THEN
        assertConstraintViolationPresentByField(errors, "recipients[0].recipientType");
        assertConstraintViolationPresentByField(errors, "recipients[0].taxId");
        assertConstraintViolationPresentByField(errors, "recipients[0].denomination");
        assertConstraintViolationPresentByField(errors, "recipients[0].physicalAddress");
        assertConstraintViolationPresentByField(errors, "notificationFeePolicy");
        Assertions.assertEquals(5, errors.size());
    }

  @Test
  @Disabled
  void validRecipientTaxIdOmocode() {
    // GIVEN
    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert(internalNotification);

        // THEN
        Assertions.assertEquals(0, errors.size());
    }

    private static NotificationPhysicalAddress createPhysicalAddress() {
        return NotificationPhysicalAddress.builder()
                .address("address")
                .zip("83100")
                .municipality("municipality")
                .build();
    }

    @Test
    @Disabled
    void invalidRecipientTaxId() {

    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert(internalNotification);

        // THEN
        assertConstraintViolationPresentByField(errors, "recipients[0].taxId");
        assertConstraintViolationPresentByField(errors, "notificationFeePolicy");
        Assertions.assertEquals(2, errors.size());
    }


  @Test
  @Disabled
  void invalidSenderTaxId() {
    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert(internalNotification);

        // THEN
        assertConstraintViolationPresentByFieldWithExpected(errors, "senderTaxId", 2);
        assertConstraintViolationPresentByField(errors, "notificationFeePolicy");
        Assertions.assertEquals(3, errors.size());
    }


  @Test
  @Disabled
  void invalidAbstract() {
    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));
    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(internalNotification);

        // THEN
        assertConstraintViolationPresentByField(errors, "abstract"); // this is a validator too, that checks what props have errors in constructor (at the moment are 2 more than the normal(3) but expect1)
        Assertions.assertEquals(1, errors.size());
    }

    @Test
    void duplicatedRecipientTaxId() {
        // Given
        NewNotificationRequestV25 n = newNotificationDuplicateRecipient();
        defaultMockConfigAndParameterForVas();

        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // Then
        assertConstraintViolationPresentByMessage(errors, "Duplicated recipient taxId");
    }

    @Test
    void applyCostNotGivenWhenNotificationIsDeliveryMode() {
        // Given
        NewNotificationRequestV25 n = newNotificationWithPaymentsWithoutApplyCosts();
        defaultMockConfigAndParameterForVas();

        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // Then
        assertConstraintViolationPresentByMessage(errors, "PagoPA applyCostFlg must be valorized for at least one payment");
        assertConstraintViolationPresentByMessage(errors, "F24 applyCostFlg must be valorized for at least one payment");

    }

    @Test
    void applyCostGivenWhenNotificationIsFlatRate() {
        // Given
        NewNotificationRequestV25 n = newNotificationWithApplyCostsAndFeePolicyFlatRate();
        defaultMockConfigAndParameterForVas();

        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // Then
        assertConstraintViolationPresentByMessage(errors, "PagoPA applyCostFlg must not be valorized for any payment");
        assertConstraintViolationPresentByMessage(errors, "F24 applyCostFlg must not be valorized for any payment");

    }

    @Test
    void validationFailsWhenNotificationHasDuplicatedIuvs() {
        // Given
        NewNotificationRequestV25 notification = newNotificationWithSameIuvs();
        defaultMockConfigAndParameterForVas();

        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(notification, notification.getSenderTaxId());

        String error = createExpectedIuvDuplicatedErrorMessage(notification, 0, 1);
        // Then
        assertConstraintViolationPresentByMessage(errors, error);

    }

    /**
     * @param n       Notifica da validare
     * @param recIdx  indice del destinatario in cui si trova il pagamento con IUV duplicato
     * @param paymIdx indice del pagamento in cui si trova lo IUV duplicato
     * @return Il messaggio d'errore di validazione per gli IUV duplicati
     */
    private String createExpectedIuvDuplicatedErrorMessage(NewNotificationRequestV25 n, int recIdx, int paymIdx) {
        NotificationPaymentItem expectedPayment = n.getRecipients().get(recIdx).getPayments().get(paymIdx);
        String expectedIuvDuplicated = expectedPayment.getPagoPa().getCreditorTaxId() + expectedPayment.getPagoPa().getNoticeCode();
        return String.format("Duplicated iuv { %s } on recipient with index %s in payment with index %s", expectedIuvDuplicated, recIdx, paymIdx);
    }


    @Test
    void denominationLengthValidationKo() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(44);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("NONE");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("adas");

        StringBuilder denomination = new StringBuilder();
        for (int i = 0; i < 45; i++) {
            denomination.append("a");
        }
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination.toString()),0);

        //THEN
        assertThat(errors, hasSize(1));
        assertThat(errors, hasItems(
                hasProperty("message", allOf(Matchers.containsString("denomination"), Matchers.containsString("recipient 0")))
        ));
    }

    @Test
    void atLengthValidationKo() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(44);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("NONE");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("adas");

        StringBuilder at = new StringBuilder();
        for (int i = 0; i < 45; i++) {
            at.append("a");
        }
        var errors = validator.checkDenomination(newNotificationAtCustom(at.toString()),0);

        //THEN
        assertThat(errors, hasSize(1));
        assertThat(errors, hasItems(
                hasProperty("message", allOf(Matchers.containsString("at"), Matchers.containsString("recipient 0")))
        ));
    }

    @Test
    void denominationLengthNOValidation() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("NONE");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("NONE");

        StringBuilder denomination = new StringBuilder();
        for (int i = 0; i < 45; i++) {
            denomination.append("a");
        }
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination.toString()),0);

        //THEN
        assertThat(errors, hasSize(0));
    }

    @Test
    void denominationLengthValidationOk() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(46);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("NONE");

        StringBuilder denomination = new StringBuilder();
        for (int i = 0; i < 45; i++) {
            denomination.append("a");
        }
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination.toString()),0);

        //THEN
        assertThat(errors, hasSize(0));
    }

    @Test
    void denominationLengthValidationOk_andExcludCharact() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(46);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("NONE");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("abc");

        StringBuilder denomination = new StringBuilder();
        for (int i = 0; i < 45; i++) {
            denomination.append("a");
        }
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination.toString()),0);

        //THEN
        assertThat(errors, hasSize(0));
    }

    @Test
    void denominationValidationIsoLatin1Ok() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("ISO_LATIN_1");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("NONE");

        String denomination = "qwertyuiopasdfghjklzxcvbnm1234567890ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination),0);

        //THEN
        assertThat(errors, hasSize(0));
    }

    @Test
    void atValidationIsoLatin1Ok() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(44);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("ISO_LATIN_1");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("NONE");

        String at = "";
        var errors = validator.checkDenomination(newNotificationAtCustom(at),0);

        //THEN
        assertThat(errors, hasSize(0));
    }


    @Test
    void denominationValidationIsoLatin1Ko() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("ISO_LATIN_1");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("|");
        String denomination = "isoLatin1okString";
        String excludedCharact = "|";
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination + excludedCharact),0);

        //THEN
        assertThat(errors, hasSize(1));
        assertThat(errors, hasItems(
                hasProperty("message", allOf(Matchers.containsString("denomination"), Matchers.containsString("recipient 0")))
        ));
    }

    @Test
    void denominationValidationIsoLatin1_AndExcludeChar_Ok() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("ISO_LATIN_1");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("|");
        String denomination = "isoLatin1okString";
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination),0);

        //THEN
        assertThat(errors, hasSize(0));
    }

    @Test
    void denominationValidationIsoLatin1_AndExcludeCharEmpty_Ok() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("ISO_LATIN_1");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("");
        String denomination = "isoLatin1okString";
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination),0);

        //THEN
        assertThat(errors, hasSize(0));
    }

    @Test
    void denominationValidationIsoLatin1_AndExcludeCharNull_Ok() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("ISO_LATIN_1");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn(null);
        String denomination = "isoLatin1okString";
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination),0);

        //THEN
        assertThat(errors, hasSize(0));
    }

    @Test
    void denominationValidationIsoLatin1_ExcludedChar_Ko() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("ISO_LATIN_1");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("q");
        String denomination = "wertyuiosdfghjklzxcvbnm1234567890ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
        String noIsoLatin1 = "Ą";
        String excludedChar = "q";
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination + noIsoLatin1 + excludedChar),0);

        //THEN
        assertThat(errors, hasSize(1));
        assertThat(errors, hasItems(
                hasProperty("message", allOf(Matchers.containsString("denomination"), Matchers.containsString("recipient 0")))
        ));
    }

    @Test
    void denominationValidationIsoLatin1_ExcludedChar_ok() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("ISO_LATIN_1");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("q");
        String denomination = "validString";
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination),0);

        //THEN
        assertThat(errors, hasSize(0));
    }

    @Test
    void denominationValidationRegexOk() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("REGEX");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("NONE");
        when(cfg.getDenominationValidationRegexValue()).thenReturn("a-zA-Z");
        when(cfg.getDenominationValidationRegexValue()).thenReturn("a-zA-Z");

        String denomination = "qwertyuiopasdfghjklzxcvbnm";
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination),0);

        //THEN
        assertThat(errors, hasSize(0));
    }


    @Test
    void denominationValidationRegexOk_AndExcludeCharactKo() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("REGEX");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("bc");
        when(cfg.getDenominationValidationRegexValue()).thenReturn("a-zA-Z");
        when(cfg.getDenominationValidationRegexValue()).thenReturn("a-zA-Z");

        String denomination = "validString";
        String excludedChar = "b";
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination + excludedChar),0);

        //THEN
        assertThat(errors, hasSize(0));
    }


    @Test
    void denominationValidationRegexKo() {
        //WHEN
        when(cfg.getDenominationLength()).thenReturn(0);
        when(cfg.getDenominationValidationTypeValue()).thenReturn("REGEX");
        when(cfg.getDenominationValidationExcludedCharacter()).thenReturn("NONE");
        when(cfg.getDenominationValidationRegexValue()).thenReturn("a-zA-Z");

        String denomination = "qwertyuiopasdfghjklzxcvbnm1234567890ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
        var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination),0);

        //THEN
        assertThat(errors, hasSize(1));
        assertThat(errors, hasItems(
                hasProperty("message", allOf(Matchers.containsString("denomination"), Matchers.containsString("recipient 0")))
        ));
    }

  @Test
  @Disabled("Documents field required in NewNotificationRequest")
  void invalidNullValuesInCollections() {
    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));
    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(internalNotification);

        // THEN
        assertConstraintViolationPresentByField(errors, "documents[0]");
        assertConstraintViolationPresentByField(errors, "recipients[0]");
        Assertions.assertEquals(2, errors.size());
    }


    @Test
    @Disabled
    void invalidDocumentAndRecipientWithEmptyFields() {

    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));

        internalNotification.notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert(internalNotification);

        // THEN
        assertConstraintViolationPresentByField(errors, "documents[0].digests");
        assertConstraintViolationPresentByField(errors, "documents[0].ref");
        assertConstraintViolationPresentByField(errors, "documents[0].contentType");
        assertConstraintViolationPresentByField(errors, "recipients[0].recipientType");
        assertConstraintViolationPresentByField(errors, "recipients[0].taxId");
        assertConstraintViolationPresentByField(errors, "recipients[0].denomination");
        assertConstraintViolationPresentByField(errors, "recipients[0].physicalAddress");
        Assertions.assertEquals(7, errors.size());
    }


    @Test
    @Disabled
    void invalidDocumentAndRecipientWithEmptyDigestsAndDigitalDomicile() {

    // GIVEN
    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.documents(List.of(it.pagopa.pn.delivery.models.internal.notification.NotificationDocument.builder()
            .ref( it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef.builder()
                    .versionToken( VERSION_TOKEN )
                    .key( KEY )
                    .build() )
            .contentType( "application/json" )
            .build()));
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert(internalNotification);

        // THEN
        assertConstraintViolationPresentByField(errors, "documents[0].digests.sha256");
        assertConstraintViolationPresentByField(errors, "documents[0].ref");
        assertConstraintViolationPresentByField(errors, "recipients[0].recipientType");
        assertConstraintViolationPresentByField(errors, "recipients[0].digitalDomicile.address");
        assertConstraintViolationPresentByField(errors, "recipients[0].digitalDomicile.type");
        assertConstraintViolationPresentByField(errors, "notificationFeePolicy");
        //assertConstraintViolationPresentByField(errors, "sourceChannel");
        // assertConstraintViolationPresentByField(errors, "recipientIds[0]");

        // here we have found some assertion to change, this expect 6 but we have added 2, to it need to expect 8, clear? yes
        // if u see al the logs error, it just say that we expect less attributes that what we have (2 attributes below) because we added 2, clear? yes
        // ok now try to fix some by your own
        Assertions.assertEquals(6, errors.size());
    }


    @Test
    void validDocumentAndRecipientWithoutPayments() {
        // GIVEN
        InternalNotification n = validDocumentWithoutPayments();
        n.notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert(n);

        // THEN
        Assertions.assertEquals(0, errors.size());
    }

    @Test
    void validDocumentAndRecipientWitPayments() {

        // GIVEN
        InternalNotification n = validDocumentWithPayments();

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert(n);

        // THEN
        Assertions.assertEquals(0, errors.size());
    }


    @Test
    @Disabled
    void testCheckNotificationDocumentFail() {

    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));
    internalNotification
            .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert(internalNotification);

        // THEN
        assertConstraintViolationPresentByField(errors, "documents[0].digests");
        assertConstraintViolationPresentByField(errors, "documents[0].ref");
        assertConstraintViolationPresentByField(errors, "documents[0].contentType");
        Assertions.assertEquals(3, errors.size());
    }

    @Test
    void newNotificationRequestWhitInvalidPhysicalAddress() {
        // GIVEN
        NewNotificationRequestV25 n = newNotification();
        n.getRecipients().get(0).setPhysicalAddress(NotificationPhysicalAddress.builder()
                .municipality("municipality")
                .address("address")
                .build()
        );

        defaultMockConfigAndParameterForVas();

        // WHEN
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // THEN
        assertConstraintViolationPresentByMessage(errors, "No province provided in physical address");
    }

    @Test
    void newNotificationRequestWhitInvalidPhysicalAddressForeignStateItaly() {
        // GIVEN
        NewNotificationRequestV25 n = newNotification();
        n.getRecipients().get(0).setPhysicalAddress(NotificationPhysicalAddress.builder()
                .foreignState("Italia")
                .municipality("municipality")
                .address("address")
                .build()
        );

        defaultMockConfigAndParameterForVas();

        // WHEN
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // THEN
        assertConstraintViolationPresentByMessage(errors, "No province provided in physical address");
    }

    @Test
    @Disabled("Since PN-2401")
        // pass all mvp checks
    void newNotificationRequestForValidDontCheckAddress() {

        // GIVEN
        NewNotificationRequestV25 n = newNotification();
        n.getRecipients().get(0).setPhysicalAddress(null);

        // WHEN
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // THEN
        Assertions.assertEquals(0, errors.size());
    }


    @Test
    @Disabled
        // doesn't pass mvp checks
    void newNotificationRequestForMVPInvalid() {

        // GIVEN
        NewNotificationRequestV25 n = newNotification();
        n.setSenderDenomination(null);
        n.addRecipientsItem(
                NotificationRecipientV24.builder().recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                        .taxId("FiscalCode").denomination("Nome Cognome / Ragione Sociale")
                        .digitalDomicile(NotificationDigitalAddress.builder().build()).build());

        // WHEN
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestForMVP(n);

        // THEN
        Assertions.assertEquals(1, errors.size());

        assertConstraintViolationPresentByMessage(errors, "Max one recipient");

    }

    @Test
        // doesn't pass mvp checks
    void newNotificationRequestForMVP() {

        // GIVEN
        NewNotificationRequestV25 n = newNotificationWithoutPayments();

        // WHEN
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestForMVP(n);

        // THEN
        Assertions.assertNotNull(errors);
        assertConstraintViolationPresentByMessage(errors, "No recipient payment");
    }

    @Test
        //positive check.
    void physicalAddressValidationOk() {

        //WHEN
        when(cfg.isPhysicalAddressValidation()).thenReturn(true);
        when(cfg.getPhysicalAddressValidationPattern()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_PATTERN);
        when(cfg.getPhysicalAddressValidationLength()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_LENGTH);
        var errors = validator.checkPhysicalAddress(newNotification().getRecipients().get(0).getPhysicalAddress(),0);

        //THEN
        assertThat(errors, empty());
    }

    @Test
        //negative check with invalid denomination field.
    void physicalAddressValidationKo() {
        //WHEN
        when(cfg.isPhysicalAddressValidation()).thenReturn(true);
        when(cfg.getPhysicalAddressValidationPattern()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_PATTERN);
        when(cfg.getPhysicalAddressValidationLength()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_LENGTH);
        var errors = validator.checkPhysicalAddress(badRecipientsNewNotification().getPhysicalAddress(),0);

        //THEN
        assertThat(errors, hasSize(1));
        assertThat(errors, hasItems(
                hasProperty("message", Matchers.containsString("exceed"))
        ));
    }

    @Test
    void physicalAddressValidationKo2() {
        //WHEN
        when(cfg.isPhysicalAddressValidation()).thenReturn(true);
        when(cfg.getPhysicalAddressValidationPattern()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_PATTERN);
        when(cfg.getPhysicalAddressValidationLength()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_LENGTH);
        var errors = validator.checkPhysicalAddress(badRecipientsNewNotification2().getPhysicalAddress(),0);

        //THEN
        assertThat(errors, hasSize(1));
    }

    @Test
    void physicalAddressValidationKo3() {
        //WHEN
        when(cfg.isPhysicalAddressValidation()).thenReturn(true);
        when(cfg.getPhysicalAddressValidationPattern()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_PATTERN);
        when(cfg.getPhysicalAddressValidationLength()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_LENGTH);
        var errors = validator.checkPhysicalAddress(badRecipientsNewNotification2().getPhysicalAddress(),0);

        //THEN
        assertThat(errors, hasSize(1));
    }


    @Test
        //negative check with all invalid fields from two different recipients.
    void PhysicalAddressMoreRecipientsValidationKo() {
        //WHEN
        when(cfg.isPhysicalAddressValidation()).thenReturn(true);
        when(cfg.getPhysicalAddressValidationPattern()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_PATTERN);
        when(cfg.getPhysicalAddressValidationLength()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_LENGTH);
        var errors = validator.checkPhysicalAddress(moreBadRecipientsNewNotification());

        //THEN
        assertThat(errors, hasSize(9));
        assertThat(errors, hasItems(
                hasProperty("message", allOf(Matchers.containsString("address"), Matchers.containsString("recipient 0"))),
                hasProperty("message", allOf(Matchers.containsString("province"), Matchers.containsString("recipient 0"))),
                hasProperty("message", allOf(Matchers.containsString("zip"), Matchers.containsString("recipient 0"))),
                hasProperty("message", allOf(Matchers.containsString("exceed"), Matchers.containsString("recipient 0"))),
                hasProperty("message", allOf(Matchers.containsString("foreignState"), Matchers.containsString("recipient 1"))),
                hasProperty("message", allOf(Matchers.containsString("addressDetails"), Matchers.containsString("recipient 1"))),
                hasProperty("message", allOf(Matchers.containsString("municipality"), Matchers.containsString("recipient 1"))),
                hasProperty("message", allOf(Matchers.containsString("at"), Matchers.containsString("recipient 1"))),
                hasProperty("message", allOf(Matchers.containsString("municipalityDetails"), Matchers.containsString("recipient 1")))
        ));
    }

    private <T> void assertConstraintViolationPresentByMessage(Set<ConstraintViolation<T>> set,
                                                               String message) {
        long actual = set.stream().filter(cv -> cv.getMessage().equals(message)).count();
        Assertions.assertEquals(1, actual);
    }

    private <T> void assertConstraintViolationPresentByField(Set<ConstraintViolation<T>> set,
                                                             String propertyPath) {
        long actual = set.stream()
                .filter(cv -> propertyPathToString(cv.getPropertyPath()).equals(propertyPath)).count();
        Assertions.assertEquals(1, actual, "expected validation errors on " + propertyPath);
    }

    private <T> void assertConstraintViolationPresentByFieldWithExpected(
            Set<ConstraintViolation<T>> set, String propertyPath, long expected) {
        long actual = set.stream()
                .filter(cv -> propertyPathToString(cv.getPropertyPath()).equals(propertyPath)).count();
        Assertions.assertEquals(expected, actual, "expected validation errors on " + propertyPath);
    }


    private void assertProblemErrorConstraintViolationPresentByField(List<ProblemError> set,
                                                                     String propertyPath) {
        long actual = set.stream()
                .filter(cv -> cv.getElement() != null && cv.getElement().equals(propertyPath)).count();
        Assertions.assertEquals(1, actual, "expected validation errors on " + propertyPath);
    }

    private static String propertyPathToString(Path propertyPath) {
        return propertyPath.toString().replaceFirst(".<[^>]*>$", "");
    }


    private NewNotificationRequestV25 newNotificationWithoutPayments() {
        NotificationRecipientV24 notificationRecipientV23 = NotificationRecipientV24.builder()
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .denomination("Ada Lovelace")
                .taxId("taxID")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at("at")
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();
        return NewNotificationRequestV25.builder()
                .notificationFeePolicy(NotificationFeePolicy.valueOf("FLAT_RATE"))
                .senderDenomination("Sender Denomination")
                .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
                .senderTaxId("paId").recipients(List.of(notificationRecipientV23)).build();
    }

    private NewNotificationRequestV25 newNotificationDuplicateRecipient() {
        NotificationRecipientV24 notificationRecipientV23 = NotificationRecipientV24.builder()
                .payments(List.of(NotificationPaymentItem.builder()
                        .pagoPa(PagoPaPayment.builder()
                                .creditorTaxId("00000000000")
                                .applyCost(false)
                                .noticeCode("000000000000000000")
                                .build())
                        .f24(F24Payment.builder()
                                .title("title")
                                .applyCost(false)
                                .metadataAttachment(NotificationMetadataAttachment.builder()
                                        .ref(NotificationAttachmentBodyRef.builder().versionToken(VERSION_TOKEN).key(KEY).build())
                                        .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
                                        .contentType("application/json")
                                        .build())
                                .build())
                        .build()))
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .denomination("Ada Lovelace")
                .taxId("taxID")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at("at")
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();
        return NewNotificationRequestV25.builder()
                .notificationFeePolicy(NotificationFeePolicy.valueOf("FLAT_RATE"))
                .senderDenomination("Sender Denomination")
                .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
                .senderTaxId("paId").recipients(List.of(notificationRecipientV23, notificationRecipientV23)).build();
    }

    private NewNotificationRequestV25 newNotificationPG() {
        NotificationRecipientV24 notificationRecipientV23 = NotificationRecipientV24.builder()
                .payments(List.of(NotificationPaymentItem.builder()
                        .pagoPa(PagoPaPayment.builder()
                                .creditorTaxId("aaaaaaaaaaa")
                                .applyCost(false)
                                .noticeCode("000000000000000000")
                                .build())
                        .f24(F24Payment.builder()
                                .title("title")
                                .applyCost(false)
                                .metadataAttachment(NotificationMetadataAttachment.builder()
                                        .ref(NotificationAttachmentBodyRef.builder().versionToken(VERSION_TOKEN).key(KEY).build())
                                        .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
                                        .contentType("application/json")
                                        .build())
                                .build())
                        .build()))
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PG)
                .denomination("Ada Lovelace")
                .taxId("taxID")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at("at")
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();
        return NewNotificationRequestV25.builder()
                .notificationFeePolicy(NotificationFeePolicy.valueOf("FLAT_RATE"))
                .senderDenomination("Sender Denomination")
                .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
                .senderTaxId("paId").recipients(List.of(notificationRecipientV23)).build();
    }

    private NewNotificationRequestV25 newNotification() {
        NotificationRecipientV24 notificationRecipientV23 = NotificationRecipientV24.builder()
                .payments(List.of(NotificationPaymentItem.builder()
                        .pagoPa(PagoPaPayment.builder()
                                .creditorTaxId("00000000000")
                                .applyCost(false)
                                .noticeCode("000000000000000000")
                                .build())
                        .f24(F24Payment.builder()
                                .title("title")
                                .applyCost(false)
                                .metadataAttachment(NotificationMetadataAttachment.builder()
                                        .ref(NotificationAttachmentBodyRef.builder().versionToken(VERSION_TOKEN).key(KEY).build())
                                        .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
                                        .contentType("application/json")
                                        .build())
                                .build())
                        .build()))
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .denomination("Ada Lovelace")
                .taxId("taxID")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at("at")
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();
        return NewNotificationRequestV25.builder()
                .notificationFeePolicy(NotificationFeePolicy.valueOf("FLAT_RATE"))
                .senderDenomination("Sender Denomination")
                .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
                .senderTaxId("paId").recipients(List.of(notificationRecipientV23)).build();
    }

    private NotificationRecipientV24 newNotificationDenominationCustom(String denomination) {
        return NotificationRecipientV24.builder()
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .denomination(denomination)
                .taxId("taxID")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at("at")
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();
    }

    private NotificationRecipientV24 newNotificationAtCustom(String at) {
       return NotificationRecipientV24.builder()
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .denomination("denomination")
                .taxId("taxID")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at(at)
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();
    }

    private NotificationRecipientV24 badRecipientsNewNotification2() {
        return NotificationRecipientV24.builder().recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .taxId("FiscalCode").denomination("Nome Cognome! / Ragione Sociale!")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
                .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo").zip("83100")
                        .province("province").municipality("municipalitymorethan40characters").at("at").build())
                .payments(List.of(NotificationPaymentItem.builder()
                                .f24(F24Payment.builder()
                                        .applyCost(true)
                                        .build())
                                .pagoPa(PagoPaPayment.builder()
                                        .applyCost(true)
                                        .noticeCode("noticeCode")
                                        .build()
                                ).build()
                        )
                )
                //.payment(NotificationPaymentInfo.builder().noticeCode("noticeCode")
                //.noticeCodeAlternative("noticeCodeAlternative").build())
                .build();
    }

    private NotificationRecipientV24 badRecipientsNewNotification() {
        return NotificationRecipientV24.builder().recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .taxId("FiscalCode").denomination("Nome Cognome! / Ragione Sociale!")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
                .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo").zip("83100")
                        .province("province").municipality("municipalitymorethan40characters").at("at").build())
                .payments(List.of(NotificationPaymentItem.builder()
                        .pagoPa(PagoPaPayment.builder()
                                .noticeCode("noticeCode")
                                .build()
                        ).build()
                )).build();
    }

  private InternalNotification validDocumentWithoutPayments() {
    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));
    return internalNotification;
  }

  private InternalNotification validDocumentWithPayments() {
    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
    internalNotification.setRecipients(Collections.singletonList(
            NotificationRecipient.builder()
                    .taxId("Codice Fiscale 01")
                    .denomination("Nome Cognome/Ragione Sociale")
                    .internalId( "recipientInternalId" )
                    .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                            .type( NotificationDigitalAddress.TypeEnum.PEC )
                            .address("account@dominio.it")
                            .build()).build()));
    return internalNotification;
  }

    private NewNotificationRequestV25 newNotificationWithPaymentsWithoutApplyCosts() {
        NotificationRecipientV24 notificationRecipientV23 = NotificationRecipientV24.builder()
                .payments(List.of(NotificationPaymentItem.builder()
                        .pagoPa(PagoPaPayment.builder()
                                .creditorTaxId("00000000000")
                                .applyCost(false)
                                .noticeCode("000000000000000000")
                                .build())
                        .f24(F24Payment.builder()
                                .title("title")
                                .applyCost(false)
                                .metadataAttachment(NotificationMetadataAttachment.builder()
                                        .ref(NotificationAttachmentBodyRef.builder().versionToken(VERSION_TOKEN).key(KEY).build())
                                        .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
                                        .contentType("application/json")
                                        .build())
                                .build())
                        .build()))
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .denomination("Ada Lovelace")
                .taxId("taxID")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at("at")
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();
        return NewNotificationRequestV25.builder()
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .senderDenomination("Sender Denomination")
                .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
                .senderTaxId("paId").recipients(List.of(notificationRecipientV23)).build();
    }

    private NewNotificationRequestV25 newNotificationWithApplyCostsAndFeePolicyFlatRate() {
        List<NotificationPaymentItem> paymentItems = new ArrayList<>();
        paymentItems.add(NotificationPaymentItem.builder()
                .pagoPa(PagoPaPayment.builder()
                        .creditorTaxId("00000000000")
                        .applyCost(true)
                        .noticeCode("000000000000000000")
                        .build())
                .f24(F24Payment.builder()
                        .title("title")
                        .applyCost(true)
                        .metadataAttachment(NotificationMetadataAttachment.builder()
                                .ref(NotificationAttachmentBodyRef.builder().versionToken(VERSION_TOKEN).key(KEY).build())
                                .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
                                .contentType("application/json")
                                .build())
                        .build())
                .build());

        NotificationRecipientV24 notificationRecipientV23 = NotificationRecipientV24.builder()
                .payments(paymentItems)
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .denomination("Ada Lovelace")
                .taxId("taxID")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at("at")
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();
        return NewNotificationRequestV25.builder()
                .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
                .senderDenomination("Sender Denomination")
                .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
                .senderTaxId("paId").recipients(List.of(notificationRecipientV23)).build();
    }

    private NewNotificationRequestV25 newNotificationWithSameIuvs() {
        NewNotificationRequestV25 notification = newNotificationWithApplyCostsAndFeePolicyFlatRate();
        NotificationPaymentItem firstPayment = notification.getRecipients().get(0).getPayments().get(0);
        NotificationPaymentItem duplicatedPayment = NotificationPaymentItem.builder()
                .pagoPa(PagoPaPayment.builder()
                        .noticeCode(firstPayment.getPagoPa().getNoticeCode())
                        .creditorTaxId(firstPayment.getPagoPa().getCreditorTaxId())
                        .build())
                .build();

        notification.getRecipients().get(0).getPayments().add(duplicatedPayment);
        return notification;
    }

    @Test
    void duplicatedAttachment() {
        String sha256 = "sha256";
        String key = "key";
        // Given
        NewNotificationRequestV25 n = newNotification();
        NotificationDocument document = NotificationDocument.builder()
                .ref(NotificationAttachmentBodyRef.builder().key(key).build())
                .digests(NotificationAttachmentDigests.builder().sha256(sha256).build())
                .build();
        n.addDocumentsItem(document);

        PagoPaPayment pagopa = n.getRecipients().get(0).getPayments().get(0).getPagoPa();
        NotificationPaymentAttachment attachment = NotificationPaymentAttachment.builder()
                .ref(NotificationAttachmentBodyRef.builder().key(key).build())
                .digests(NotificationAttachmentDigests.builder().sha256(sha256).build())
                .build();
        pagopa.setAttachment(attachment);

        defaultMockConfigAndParameterForVas();

        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // Then
        assertConstraintViolationPresentByMessage(errors, "Same attachment compares more then once in the same request");
    }

    @Test
    void duplicatedAttachmentNotImpactMoreRecipients() {
        String sha256 = "sha256";
        String key = "key";
        // Given
        NewNotificationRequestV25 n = newNotification();
        NotificationDocument document = NotificationDocument.builder()
                .ref(NotificationAttachmentBodyRef.builder().key("key1").build())
                .digests(NotificationAttachmentDigests.builder().sha256(sha256).build())
                .build();
        n.addDocumentsItem(document);

        NotificationRecipientV24 recipient = NotificationRecipientV24.builder()
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .denomination("Ada Lovelace")
                .taxId("taxID")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at("at")
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();

        NotificationRecipientV24 recipient2 = NotificationRecipientV24.builder()
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .denomination("Mario Rossi")
                .taxId("taxID")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at("at")
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();

        recipient.setPayments(Arrays.asList(NotificationPaymentItem.builder()
                .pagoPa(PagoPaPayment.builder()
                        .attachment(NotificationPaymentAttachment.builder()
                                .ref(NotificationAttachmentBodyRef.builder().key(key).build())
                                .digests(NotificationAttachmentDigests.builder().sha256(sha256).build())
                                .build())
                        .build())
                .build()));

        recipient2.setPayments(Arrays.asList(NotificationPaymentItem.builder()
                .pagoPa(PagoPaPayment.builder()
                        .attachment(NotificationPaymentAttachment.builder()
                                .ref(NotificationAttachmentBodyRef.builder().key(key).build())
                                .digests(NotificationAttachmentDigests.builder().sha256(sha256).build())
                                .build())
                        .build())
                .build()));

        n.setRecipients(Arrays.asList(recipient, recipient2));

        defaultMockConfigAndParameterForVas();


        // When
        Set<ConstraintViolation<NewNotificationRequestV25>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert(n, n.getSenderTaxId());

        // Then
        long actual = errors.stream().filter(cv -> cv.getMessage().equals("Same attachment compares more then once in the same request")).count();
        Assertions.assertEquals(0, actual);
    }

    @Test
    void checkNewNotificationRequestBeforeInsertAndThrow_validRequest_noErrors() {
        String sha256 = "cvZKB4NCsHjo0stdb47gnfx0/Hjiipov0+M9oXcJT2Y=";
        NewNotificationRequestV25 validRequest = getNewNotificationRequestV24(sha256);

        when(validateUtils.validate("26188370808", false, false, false)).thenReturn(true);
        when(mvpParameterConsumer.isMvp(validRequest.getSenderTaxId())).thenReturn(false);

        defaultMockConfigAndParameterForVas();

        assertDoesNotThrow(() -> validator.checkNewNotificationRequestBeforeInsertAndThrow(validRequest, validRequest.getSenderTaxId()));
    }

    @Test
    void checkNewNotificationRequestBeforeInsertAndThrow_withPhysicalAddressLookupStartNull() {
        String sha256 = "cvZKB4NCsHjo0stdb47gnfx0/Hjiipov0+M9oXcJT2Y=";
        NewNotificationRequestV25 validRequest = getNewNotificationRequestV24(sha256);
        when(cfg.getPhysicalAddressLookupStartDate()).thenReturn(null);

        when(validateUtils.validate("26188370808", false, false, false)).thenReturn(true);
        when(mvpParameterConsumer.isMvp(validRequest.getSenderTaxId())).thenReturn(false);

        defaultMockConfigAndParameterForVas();

        assertDoesNotThrow(() -> validator.checkNewNotificationRequestBeforeInsertAndThrow(validRequest, validRequest.getSenderTaxId()));
    }

    @Test
    void checkNewNotificationRequestBeforeInsertAndThrow_WithInvalidAdditionalLang() {
        String sha256 = "cvZKB4NCsHjo0stdb47gnfx0/Hjiipov0+M9oXcJT2Y=";
        NewNotificationRequestV25 validRequest = getNewNotificationRequestV24(sha256);
        validRequest.setAdditionalLanguages(List.of("EN"));

        when(validateUtils.validate("26188370808", false, false, false)).thenReturn(true);
        when(mvpParameterConsumer.isMvp(validRequest.getSenderTaxId())).thenReturn(false);

        defaultMockConfigAndParameterForVas();

        Assertions.assertThrows(PnBadRequestException.class, () -> validator.checkNewNotificationRequestBeforeInsertAndThrow(validRequest, validRequest.getSenderTaxId()),
                "Lingua aggiuntiva non valida, i valori accettati sono DE,FR,SL");
    }

    @Test
    void checkNewNotificationRequestBeforeInsertAndThrow_WithMultipleAdditionalLang() {
        String sha256 = "cvZKB4NCsHjo0stdb47gnfx0/Hjiipov0+M9oXcJT2Y=";
        NewNotificationRequestV25 validRequest = getNewNotificationRequestV24(sha256);
        validRequest.setAdditionalLanguages(List.of("DE", "SL"));
        when(validateUtils.validate("26188370808", false, false, false)).thenReturn(true);
        when(mvpParameterConsumer.isMvp(validRequest.getSenderTaxId())).thenReturn(false);

        defaultMockConfigAndParameterForVas();

        Assertions.assertThrows(PnBadRequestException.class, () -> validator.checkNewNotificationRequestBeforeInsertAndThrow(validRequest, validRequest.getSenderTaxId()),
                "È obbligatorio fornire una sola lingua aggiuntiva.");
    }

    @Test
    void checkNewNotificationRequestBeforeInsertAndThrow_WithValidAdditionalLang() {
        String sha256 = "cvZKB4NCsHjo0stdb47gnfx0/Hjiipov0+M9oXcJT2Y=";
        NewNotificationRequestV25 validRequest = getNewNotificationRequestV24(sha256);
        validRequest.setAdditionalLanguages(List.of("DE"));
        when(validateUtils.validate("26188370808", false, false, false)).thenReturn(true);
        when(mvpParameterConsumer.isMvp(validRequest.getSenderTaxId())).thenReturn(false);
        defaultMockConfigAndParameterForVas();

        assertDoesNotThrow(() -> validator.checkNewNotificationRequestBeforeInsertAndThrow(validRequest, validRequest.getSenderTaxId()));
    }

    @NotNull
    private static NewNotificationRequestV25 getNewNotificationRequestV24(String sha256) {
        NewNotificationRequestV25 validRequest = new NewNotificationRequestV25();

        validRequest.setSenderTaxId("12345678958");
        validRequest.setSenderDenomination("sender");
        validRequest.setSubject("sub");

        NotificationRecipientV24 recipient = NotificationRecipientV24.builder()
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PG)
                .denomination("Mario Rossi")
                .taxId("26188370808")
                .digitalDomicile(NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("address@pec.it")
                        .build())
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .at("at")
                        .province("province")
                        .zip("83100")
                        .address("address")
                        .addressDetails("addressDetail")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetail")
                        .build())
                .build();

        NotificationDocument document = NotificationDocument.builder()
                .ref(NotificationAttachmentBodyRef.builder().key("key1").versionToken("token").build())
                .digests(NotificationAttachmentDigests.builder().sha256(sha256).build())
                .contentType("application/pdf")
                .build();
        validRequest.addDocumentsItem(document);

        validRequest.setRecipients(List.of(recipient));
        validRequest.setNotificationFeePolicy(NotificationFeePolicy.FLAT_RATE);
        validRequest.setPaFee(1);
        validRequest.setVat(90);
        validRequest.setTaxonomyCode("123456A");
        validRequest.setPaProtocolNumber("prot");
        validRequest.setPhysicalCommunicationType(NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        return validRequest;
    }
}

