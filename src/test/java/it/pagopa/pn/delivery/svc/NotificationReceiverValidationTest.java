package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.time.OffsetDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
@Slf4j
class NotificationReceiverValidationTest {

  @Mock
  private PnDeliveryConfigs cfg;

  @Mock
  private MVPParameterConsumer mvpParameterConsumer;

  @Mock
  private ValidateUtils validateUtils;

  private static final String IUN = "FAKE-FAKE-FAKE-202209-F-1";

  private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
  public static final String SHA256_BODY = "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=";
  public static final String VERSION_TOKEN = "version_token";
  public static final String KEY = "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG"; // or also PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG
  public static final String PHYSICAL_ADDRESS_VALIDATION_PATTERN = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ./ '-";
  public static final Integer PHYSICAL_ADDRESS_VALIDATION_LENGTH = 44;

  private NotificationReceiverValidator validator;

  @BeforeEach
  void initializeValidator() {
    this.cfg = Mockito.mock(PnDeliveryConfigs.class);
    this.validateUtils = Mockito.mock( ValidateUtils.class );
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = new NotificationReceiverValidator(factory.getValidator(), mvpParameterConsumer, validateUtils, cfg);
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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
  @Disabled
  void invalidRecipientPGTaxId() {
    // Given
    NewNotificationRequestV23 n = newNotificationPG();
    // When
    Set<ConstraintViolation<NewNotificationRequestV23>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // Then
    assertConstraintViolationPresentByMessage(errors, "SEND accepts only numerical taxId for PG recipient 1");
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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
    NewNotificationRequestV23 n = newNotificationDuplicateRecipient();

    // When
    Set<ConstraintViolation<NewNotificationRequestV23>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // Then
    assertConstraintViolationPresentByMessage(errors, "Duplicated recipient taxId");
  }

  @Test
  void applyCostNotGivenWhenNotificationIsDeliveryMode() {
    // Given
    NewNotificationRequestV23 n = newNotificationWithPaymentsWithoutApplyCosts();

    // When
    Set<ConstraintViolation<NewNotificationRequestV23>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // Then
    assertConstraintViolationPresentByMessage(errors, "PagoPA applyCostFlg must be valorized for at least one payment");
    assertConstraintViolationPresentByMessage(errors, "F24 applyCostFlg must be valorized for at least one payment");

  }

  @Test
  void applyCostGivenWhenNotificationIsFlatRate() {
    // Given
    NewNotificationRequestV23 n = newNotificationWithApplyCostsAndFeePolicyFlatRate();

    // When
    Set<ConstraintViolation<NewNotificationRequestV23>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // Then
    assertConstraintViolationPresentByMessage(errors, "PagoPA applyCostFlg must not be valorized for any payment");
    assertConstraintViolationPresentByMessage(errors, "F24 applyCostFlg must not be valorized for any payment");

  }

  @Test
  void validationFailsWhenNotificationHasDuplicatedIuvs() {
    // Given
    NewNotificationRequestV23 notification = newNotificationWithSameIuvs();

    // When
    Set<ConstraintViolation<NewNotificationRequestV23>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(notification);

    String error = createExpectedIuvDuplicatedErrorMessage(notification, 0, 1);
    // Then
    assertConstraintViolationPresentByMessage(errors, error);

  }

  /**
   *
   * @param n Notifica da validare
   * @param recIdx indice del destinatario in cui si trova il pagamento con IUV duplicato
   * @param paymIdx indice del pagamento in cui si trova lo IUV duplicato
   * @return Il messaggio d'errore di validazione per gli IUV duplicati
   */
  private String createExpectedIuvDuplicatedErrorMessage(NewNotificationRequestV23 n, int recIdx, int paymIdx) {
    NotificationPaymentItem expectedPayment = n.getRecipients().get(recIdx).getPayments().get(paymIdx);
    String expectedIuvDuplicated = expectedPayment.getPagoPa().getCreditorTaxId() + expectedPayment.getPagoPa().getNoticeCode();
    return String.format("Duplicated iuv { %s } on recipient with index %s in payment with index %s", expectedIuvDuplicated, recIdx, paymIdx);
  }


  @Test
  void denominationLengthValidationKo() {
    //WHEN
    when(cfg.getDenominationLength()).thenReturn(44);
    when(cfg.getDenominationValidationTypeValue()).thenReturn("NONE");

    StringBuilder denomination = new StringBuilder();
    for(int i = 0; i < 45; i++){
      denomination.append("a");
    }
    var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination.toString()));

    //THEN
    assertThat(errors, hasSize(1));
    assertThat(errors, hasItems(
            hasProperty("message", allOf(Matchers.containsString("denomination"), Matchers.containsString("recipient 0")))
    ));
  }

  @Test
  void denominationLengthNOValidation() {
    //WHEN
    when(cfg.getDenominationLength()).thenReturn(0);
    when(cfg.getDenominationValidationTypeValue()).thenReturn("NONE");

    StringBuilder denomination = new StringBuilder();
    for(int i = 0; i < 45; i++){
      denomination.append("a");
    }
    var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination.toString()));

    //THEN
    assertThat(errors, hasSize(0));
  }

  @Test
  void denominationLengthValidationOk() {
    //WHEN
    when(cfg.getDenominationLength()).thenReturn(46);
    when(cfg.getDenominationValidationTypeValue()).thenReturn("NONE");

    StringBuilder denomination = new StringBuilder();
    for(int i = 0; i < 45; i++){
      denomination.append("a");
    }
    var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination.toString()));

    //THEN
    assertThat(errors, hasSize(0));
  }

  @Test
  void denominationValidationIsoLatin1Ok() {
    //WHEN
    when(cfg.getDenominationLength()).thenReturn(0);
    when(cfg.getDenominationValidationTypeValue()).thenReturn("ISO_LATIN_1");

    String denomination = "qwertyuiopasdfghjklzxcvbnm1234567890ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
    var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination));

    //THEN
    assertThat(errors, hasSize(0));
  }

  @Test
  void denominationValidationIsoLatin1Ko() {
    //WHEN
    when(cfg.getDenominationLength()).thenReturn(0);
    when(cfg.getDenominationValidationTypeValue()).thenReturn("ISO_LATIN_1");

    String denomination = "qwertyuiopasdfghjklzxcvbnm1234567890ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
    String noIsoLatin1 = "Ą";
    var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination+noIsoLatin1));

    //THEN
    assertThat(errors, hasSize(1));
    assertThat(errors, hasItems(
            hasProperty("message", allOf(Matchers.containsString("denomination"), Matchers.containsString("recipient 0")))
    ));
  }

  @Test
  void denominationValidationRegexOk() {
    //WHEN
    when(cfg.getDenominationLength()).thenReturn(0);
    when(cfg.getDenominationValidationTypeValue()).thenReturn("REGEX");
    when(cfg.getDenominationValidationRegexValue()).thenReturn("a-zA-Z");

    String denomination = "qwertyuiopasdfghjklzxcvbnm";
    var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination));

    //THEN
    assertThat(errors, hasSize(0));
  }

  @Test
  void denominationValidationRegexKo() {
    //WHEN
    when(cfg.getDenominationLength()).thenReturn(0);
    when(cfg.getDenominationValidationTypeValue()).thenReturn("REGEX");
    when(cfg.getDenominationValidationRegexValue()).thenReturn("a-zA-Z");

    String denomination = "qwertyuiopasdfghjklzxcvbnm1234567890ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
    var errors = validator.checkDenomination(newNotificationDenominationCustom(denomination));

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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
    NewNotificationRequestV23 n = newNotification();
    n.getRecipients().get(0).setPhysicalAddress(NotificationPhysicalAddress.builder()
            .municipality( "municipality" )
            .address( "address" )
            .build()
    );

    // WHEN
    Set<ConstraintViolation<NewNotificationRequestV23>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByMessage(errors, "No province provided in physical address");
  }

  @Test
  void newNotificationRequestWhitInvalidPhysicalAddressForeignStateItaly() {
    // GIVEN
    NewNotificationRequestV23 n = newNotification();
    n.getRecipients().get(0).setPhysicalAddress(NotificationPhysicalAddress.builder()
            .foreignState("Italia")
            .municipality( "municipality" )
            .address( "address" )
            .build()
    );

    // WHEN
    Set<ConstraintViolation<NewNotificationRequestV23>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByMessage(errors, "No province provided in physical address");
  }

  @Test
  @Disabled("Since PN-2401")
    // pass all mvp checks
  void newNotificationRequestForValidDontCheckAddress() {

    // GIVEN
    NewNotificationRequestV23 n = newNotification();
    n.getRecipients().get(0).setPhysicalAddress(null);

    // WHEN
    Set<ConstraintViolation<NewNotificationRequestV23>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // THEN
    Assertions.assertEquals(0, errors.size());
  }



  @Test
  @Disabled
    // doesn't pass mvp checks
  void newNotificationRequestForMVPInvalid() {

    // GIVEN
    NewNotificationRequestV23 n = newNotification();
    n.setSenderDenomination(null);
    n.addRecipientsItem(
            NotificationRecipientV23.builder().recipientType(NotificationRecipientV23.RecipientTypeEnum.PF)
                    .taxId("FiscalCode").denomination("Nome Cognome / Ragione Sociale")
                    .digitalDomicile(NotificationDigitalAddress.builder().build()).build());

    // WHEN
    Set<ConstraintViolation<NewNotificationRequestV23>> errors;
    errors = validator.checkNewNotificationRequestForMVP(n);

    // THEN
    Assertions.assertEquals(1, errors.size());

    assertConstraintViolationPresentByMessage(errors, "Max one recipient");

  }

  @Test
    // doesn't pass mvp checks
  void newNotificationRequestForMVP() {

    // GIVEN
    NewNotificationRequestV23 n = newNotificationWithoutPayments();

    // WHEN
    Set<ConstraintViolation<NewNotificationRequestV23>> errors;
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
    var errors = validator.checkPhysicalAddress(newNotification());

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
    var errors = validator.checkPhysicalAddress(badRecipientsNewNotification());

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
    var errors = validator.checkPhysicalAddress(badRecipientsNewNotification2());

    //THEN
    assertThat(errors, hasSize(1));
  }

  @Test
  void physicalAddressValidationKo3() {
    //WHEN
    when(cfg.isPhysicalAddressValidation()).thenReturn(true);
    when(cfg.getPhysicalAddressValidationPattern()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_PATTERN);
    when(cfg.getPhysicalAddressValidationLength()).thenReturn(PHYSICAL_ADDRESS_VALIDATION_LENGTH);
    var errors = validator.checkPhysicalAddress(badRecipientsNewNotification2());

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


  private NewNotificationRequestV23 newNotificationWithoutPayments() {
    NotificationRecipientV23 notificationRecipientV23 = NotificationRecipientV23.builder()
            .recipientType( NotificationRecipientV23.RecipientTypeEnum.PF )
            .denomination( "Ada Lovelace" )
            .taxId( "taxID" )
            .digitalDomicile( NotificationDigitalAddress.builder()
                    .type( NotificationDigitalAddress.TypeEnum.PEC )
                    .address( "address@pec.it" )
                    .build() )
            .physicalAddress( NotificationPhysicalAddress.builder()
                    .at( "at" )
                    .province( "province" )
                    .zip( "83100" )
                    .address( "address" )
                    .addressDetails( "addressDetail" )
                    .municipality( "municipality" )
                    .municipalityDetails( "municipalityDetail" )
                    .build() )
            .build();
    return NewNotificationRequestV23.builder()
            .notificationFeePolicy(NotificationFeePolicy.valueOf("FLAT_RATE"))
            .senderDenomination("Sender Denomination")
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(List.of(notificationRecipientV23)).build();
  }

  private NewNotificationRequestV23 newNotificationDuplicateRecipient() {
    NotificationRecipientV23 notificationRecipientV23 = NotificationRecipientV23.builder()
            .payments( List.of(NotificationPaymentItem.builder()
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
            .recipientType( NotificationRecipientV23.RecipientTypeEnum.PF )
            .denomination( "Ada Lovelace" )
            .taxId( "taxID" )
            .digitalDomicile( NotificationDigitalAddress.builder()
                    .type( NotificationDigitalAddress.TypeEnum.PEC )
                    .address( "address@pec.it" )
                    .build() )
            .physicalAddress( NotificationPhysicalAddress.builder()
                    .at( "at" )
                    .province( "province" )
                    .zip( "83100" )
                    .address( "address" )
                    .addressDetails( "addressDetail" )
                    .municipality( "municipality" )
                    .municipalityDetails( "municipalityDetail" )
                    .build() )
            .build();
    return NewNotificationRequestV23.builder()
            .notificationFeePolicy(NotificationFeePolicy.valueOf("FLAT_RATE"))
            .senderDenomination("Sender Denomination")
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(List.of(notificationRecipientV23, notificationRecipientV23)).build();
  }

  private NewNotificationRequestV23 newNotificationPG() {
    NotificationRecipientV23 notificationRecipientV23 = NotificationRecipientV23.builder()
            .payments( List.of(NotificationPaymentItem.builder()
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
            .recipientType( NotificationRecipientV23.RecipientTypeEnum.PG )
            .denomination( "Ada Lovelace" )
            .taxId( "taxID" )
            .digitalDomicile( NotificationDigitalAddress.builder()
                    .type( NotificationDigitalAddress.TypeEnum.PEC )
                    .address( "address@pec.it" )
                    .build() )
            .physicalAddress( NotificationPhysicalAddress.builder()
                    .at( "at" )
                    .province( "province" )
                    .zip( "83100" )
                    .address( "address" )
                    .addressDetails( "addressDetail" )
                    .municipality( "municipality" )
                    .municipalityDetails( "municipalityDetail" )
                    .build() )
            .build();
    return NewNotificationRequestV23.builder()
            .notificationFeePolicy(NotificationFeePolicy.valueOf("FLAT_RATE"))
            .senderDenomination("Sender Denomination")
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(List.of(notificationRecipientV23)).build();
  }

  private NewNotificationRequestV23 newNotification() {
    NotificationRecipientV23 notificationRecipientV23 = NotificationRecipientV23.builder()
            .payments( List.of(NotificationPaymentItem.builder()
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
            .recipientType( NotificationRecipientV23.RecipientTypeEnum.PF )
            .denomination( "Ada Lovelace" )
            .taxId( "taxID" )
            .digitalDomicile( NotificationDigitalAddress.builder()
                    .type( NotificationDigitalAddress.TypeEnum.PEC )
                    .address( "address@pec.it" )
                    .build() )
            .physicalAddress( NotificationPhysicalAddress.builder()
                    .at( "at" )
                    .province( "province" )
                    .zip( "83100" )
                    .address( "address" )
                    .addressDetails( "addressDetail" )
                    .municipality( "municipality" )
                    .municipalityDetails( "municipalityDetail" )
                    .build() )
            .build();
    return NewNotificationRequestV23.builder()
            .notificationFeePolicy(NotificationFeePolicy.valueOf("FLAT_RATE"))
            .senderDenomination("Sender Denomination")
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(List.of(notificationRecipientV23)).build();
  }

  private NewNotificationRequestV23 newNotificationDenominationCustom(String denomination) {
    NotificationRecipientV23 notificationRecipient = NotificationRecipientV23.builder()
            .recipientType( NotificationRecipientV23.RecipientTypeEnum.PF )
            .denomination( denomination )
            .taxId( "taxID" )
            .digitalDomicile( NotificationDigitalAddress.builder()
                    .type( NotificationDigitalAddress.TypeEnum.PEC )
                    .address( "address@pec.it" )
                    .build() )
            .physicalAddress( NotificationPhysicalAddress.builder()
                    .at( "at" )
                    .province( "province" )
                    .zip( "83100" )
                    .address( "address" )
                    .addressDetails( "addressDetail" )
                    .municipality( "municipality" )
                    .municipalityDetails( "municipalityDetail" )
                    .build() )
            .build();
    return NewNotificationRequestV23.builder().senderDenomination("Sender Denomination")
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(Arrays.asList(notificationRecipient)).build();
  }

  private FullSentNotificationV23 newFullSentNotification() {
    return FullSentNotificationV23.builder().sentAt(OffsetDateTime.now()).iun(IUN)
        .paProtocolNumber("protocol1").group("group_1").idempotenceToken("idempotenceToken")
        .timeline(Collections.singletonList(TimelineElementV23.builder().build()))
        .notificationStatus(NotificationStatus.ACCEPTED)
        .documents(Collections.singletonList(NotificationDocument.builder()
            .contentType("application/pdf")
            .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                .build())
            .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build()))
        .recipients(Collections.singletonList(NotificationRecipientV23.builder()
            .taxId("LVLDAA85T50G702B").recipientType(NotificationRecipientV23.RecipientTypeEnum.PF)
            .denomination("Ada Lovelace")
            .digitalDomicile(NotificationDigitalAddress.builder().address("indirizzo@pec.it")
                .type(NotificationDigitalAddress.TypeEnum.PEC).build())
                .physicalAddress( createPhysicalAddress() )
                .build())
        )
        .notificationStatusHistory(Collections.singletonList(NotificationStatusHistoryElement
            .builder().activeFrom(OffsetDateTime.now()).status(NotificationStatus.ACCEPTED)
            .relatedTimelineElements(Collections.emptyList()).build()))
        .senderDenomination("Comune di Milano").senderTaxId("01199250158").subject("subject_length")
        .sourceChannel(X_PAGOPA_PN_SRC_CH)
        .physicalCommunicationType(
                FullSentNotificationV23.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
        .build();
  }

  private NewNotificationRequestV23 badRecipientsNewNotification2() {
    List<NotificationRecipientV23> recipients = new ArrayList<>();
    recipients.add(
            NotificationRecipientV23.builder().recipientType(NotificationRecipientV23.RecipientTypeEnum.PF)
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
                    .build());
    return NewNotificationRequestV23.builder().senderDenomination("Sender Denomination")
            .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(recipients).build();
  }

  private NewNotificationRequestV23 badRecipientsNewNotification() {
    List<NotificationRecipientV23> recipients = new ArrayList<>();
    recipients.add(
            NotificationRecipientV23.builder().recipientType(NotificationRecipientV23.RecipientTypeEnum.PF)
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
                            )
                    )
                    //.payment(NotificationPaymentInfo.builder().noticeCode("noticeCode")
                    //.noticeCodeAlternative("noticeCodeAlternative").build())
                    .build());
    return NewNotificationRequestV23.builder().senderDenomination("Sender Denomination")
            .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(recipients).build();
  }

  private NewNotificationRequestV23 moreBadRecipientsNewNotification() {
    List<NotificationRecipientV23> recipients = new ArrayList<>();
    recipients.add(NotificationRecipientV23.builder().recipientType(NotificationRecipientV23.RecipientTypeEnum.PF)
            .taxId("FiscalCode").denomination("Nome Cognome! / Ragione Sociale!")
            .digitalDomicile(NotificationDigitalAddress.builder()
                    .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
            .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo?").zip("83100*")
                    .province("province_").municipality("municipalitymorethan40characters-").municipalityDetails("municipalityDetails/")
                    .at("at.").addressDetails("addressDetails0").foreignState("foreignState ").build())
            .payments(List.of(NotificationPaymentItem.builder()
                            .pagoPa(PagoPaPayment.builder()
                                    .noticeCode("noticeCode")
                                    .applyCost(false)
                                    .build()
                            ).build()
                    )
            )
            .build());
    recipients.add(NotificationRecipientV23.builder().recipientType(NotificationRecipientV23.RecipientTypeEnum.PF)
            .taxId("FiscalCode").denomination("Nome Cognome / Ragione Sociale")
            .digitalDomicile(NotificationDigitalAddress.builder()
                    .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
            .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo").zip("83100")
                    .province("province").municipality("municipality!").municipalityDetails("municipalityDetails?")
                    .at("at_").addressDetails("addressDetails$").foreignState("foreignState%").build())
            .payments(List.of(NotificationPaymentItem.builder()
                            .pagoPa(PagoPaPayment.builder()
                                    .noticeCode("noticeCode")
                                    .applyCost(false)
                                    .build()
                            ).build()
                    )
            )
            .build());
    return NewNotificationRequestV23.builder().senderDenomination("Sender Denomination")
            .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(recipients).build();
  }

  private InternalNotification newInternalNotification() {
    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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

  private InternalNotification notificationWithPhysicalCommunicationType() {
    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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

  private InternalNotification validDocumentWithoutPayments() {
    InternalNotification internalNotification = new InternalNotification();
    internalNotification.setIun("IUN");
    internalNotification.setPaProtocolNumber("protocol_01");
    internalNotification.setSubject("Subject 01");
    internalNotification.setCancelledIun("IUN_05");
    internalNotification.setCancelledIun("IUN_00");
    internalNotification.setSenderPaId("PA_ID");
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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
    internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
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

  private NewNotificationRequestV23 newNotificationWithPaymentsWithoutApplyCosts() {
    NotificationRecipientV23 notificationRecipientV23 = NotificationRecipientV23.builder()
            .payments( List.of(NotificationPaymentItem.builder()
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
            .recipientType( NotificationRecipientV23.RecipientTypeEnum.PF )
            .denomination( "Ada Lovelace" )
            .taxId( "taxID" )
            .digitalDomicile( NotificationDigitalAddress.builder()
                    .type( NotificationDigitalAddress.TypeEnum.PEC )
                    .address( "address@pec.it" )
                    .build() )
            .physicalAddress( NotificationPhysicalAddress.builder()
                    .at( "at" )
                    .province( "province" )
                    .zip( "83100" )
                    .address( "address" )
                    .addressDetails( "addressDetail" )
                    .municipality( "municipality" )
                    .municipalityDetails( "municipalityDetail" )
                    .build() )
            .build();
    return NewNotificationRequestV23.builder()
            .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
            .senderDenomination("Sender Denomination")
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(List.of(notificationRecipientV23)).build();
  }

  private NewNotificationRequestV23 newNotificationWithApplyCostsAndFeePolicyFlatRate() {
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

    NotificationRecipientV23 notificationRecipientV23 = NotificationRecipientV23.builder()
            .payments(paymentItems)
            .recipientType( NotificationRecipientV23.RecipientTypeEnum.PF )
            .denomination( "Ada Lovelace" )
            .taxId( "taxID" )
            .digitalDomicile( NotificationDigitalAddress.builder()
                    .type( NotificationDigitalAddress.TypeEnum.PEC )
                    .address( "address@pec.it" )
                    .build() )
            .physicalAddress( NotificationPhysicalAddress.builder()
                    .at( "at" )
                    .province( "province" )
                    .zip( "83100" )
                    .address( "address" )
                    .addressDetails( "addressDetail" )
                    .municipality( "municipality" )
                    .municipalityDetails( "municipalityDetail" )
                    .build() )
            .build();
    return NewNotificationRequestV23.builder()
            .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
            .senderDenomination("Sender Denomination")
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(List.of(notificationRecipientV23)).build();
  }

  private NewNotificationRequestV23 newNotificationWithSameIuvs() {
    NewNotificationRequestV23 notification = newNotificationWithApplyCostsAndFeePolicyFlatRate();
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

}

