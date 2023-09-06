package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.validation.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
  public static final String INVALID_ABSTRACT =
          "invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars";
  public static final String INVALID_SUBJECT =
          "invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars";


  private NotificationReceiverValidator validator;

  @BeforeEach
  void initializeValidator() {
    this.cfg = Mockito.mock(PnDeliveryConfigs.class);
    this.validateUtils = Mockito.mock( ValidateUtils.class );
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = new NotificationReceiverValidator(factory.getValidator(), mvpParameterConsumer, validateUtils, cfg);
  }

  @Test
  void invalidEmptyNotification() {

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
    assertConstraintViolationPresentByField(errors, "recipients");
    assertConstraintViolationPresentByField(errors, "timeline");
    assertConstraintViolationPresentByField(errors, "notificationStatusHistory");
    assertConstraintViolationPresentByField(errors, "documents");
    assertConstraintViolationPresentByField(errors, "iun");
    assertConstraintViolationPresentByField(errors, "notificationStatus");
    assertConstraintViolationPresentByField(errors, "sentAt");
    assertConstraintViolationPresentByField(errors, "paProtocolNumber");
    assertConstraintViolationPresentByField(errors, "physicalCommunicationType");
    assertConstraintViolationPresentByField(errors, "subject");
    assertConstraintViolationPresentByField(errors, "notificationFeePolicy");
    assertConstraintViolationPresentByField(errors, "senderDenomination");
    assertConstraintViolationPresentByField(errors, "senderTaxId");
    Assertions.assertEquals(13, errors.size());
  }

  @Test
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
    Executable todo = () -> validator.checkNewNotificationBeforeInsertAndThrow(internalNotification);

    // THEN
    PnValidationException validationException;
    validationException = Assertions.assertThrows(PnValidationException.class, todo);

    @NotNull
    @Valid
    @Size(min = 1)
    List<ProblemError> errors = validationException.getProblem().getErrors();
    assertProblemErrorConstraintViolationPresentByField(errors, "recipients");
    assertProblemErrorConstraintViolationPresentByField(errors, "timeline");
    assertProblemErrorConstraintViolationPresentByField(errors, "notificationStatusHistory");
    assertProblemErrorConstraintViolationPresentByField(errors, "documents");
    assertProblemErrorConstraintViolationPresentByField(errors, "iun");
    assertProblemErrorConstraintViolationPresentByField(errors, "notificationStatus");
    assertProblemErrorConstraintViolationPresentByField(errors, "sentAt");
    assertProblemErrorConstraintViolationPresentByField(errors, "paProtocolNumber");
    assertProblemErrorConstraintViolationPresentByField(errors, "physicalCommunicationType");
    assertProblemErrorConstraintViolationPresentByField(errors, "subject");
    assertProblemErrorConstraintViolationPresentByField(errors, "notificationFeePolicy");
    assertProblemErrorConstraintViolationPresentByField(errors, "senderDenomination");
    assertProblemErrorConstraintViolationPresentByField(errors, "senderTaxId");
    Assertions.assertEquals(13, errors.size());
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
  void invalidRecipientPGTaxId() {
    // Given
    NewNotificationRequestV21 n = newNotification();
    n.addRecipientsItem(
            NotificationRecipientV21.builder().recipientType(NotificationRecipientV21.RecipientTypeEnum.PG)
                    .taxId("1234c56").denomination("recipientDenomination").build());

    // When
    Set<ConstraintViolation<NewNotificationRequestV21>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // Then
    assertConstraintViolationPresentByMessage(errors, "SEND accepts only numerical taxId for PG recipient 1");
  }

  @Test
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
  void invalidSubject() {
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
    assertConstraintViolationPresentByField(errors, "subject");
    Assertions.assertEquals(1, errors.size());
  }

  @Test
  void duplicatedRecipientTaxId() {
    // Given
    NewNotificationRequestV21 n = newNotification();
    n.addRecipientsItem(
            NotificationRecipientV21.builder().recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
                    .taxId("FiscalCode").denomination("recipientDenomination").build());

    // When
    Set<ConstraintViolation<NewNotificationRequestV21>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // Then
    assertConstraintViolationPresentByMessage(errors, "Duplicated recipient taxId");
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
  void invalidPecAddress() {
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
    assertConstraintViolationPresentByField(errors, "recipients[0].digitalDomicile.address");
    Assertions.assertEquals(1, errors.size());
  }


  @Test
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
    NewNotificationRequestV21 n = newNotification();
    n.getRecipients().get(0).setPhysicalAddress(NotificationPhysicalAddress.builder()
            .municipality( "municipality" )
            .address( "address" )
            .build()
    );

    // WHEN
    Set<ConstraintViolation<NewNotificationRequestV21>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByMessage(errors, "No province provided in physical address");
  }

  @Test
  void newNotificationRequestWhitInvalidPhysicalAddressForeignStateItaly() {
    // GIVEN
    NewNotificationRequestV21 n = newNotification();
    n.getRecipients().get(0).setPhysicalAddress(NotificationPhysicalAddress.builder()
            .foreignState("Italia")
            .municipality( "municipality" )
            .address( "address" )
            .build()
    );

    // WHEN
    Set<ConstraintViolation<NewNotificationRequestV21>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByMessage(errors, "No province provided in physical address");
  }

  @Test
  @Disabled("Since PN-2401")
    // pass all mvp checks
  void newNotificationRequestForValidDontCheckAddress() {

    // GIVEN
    NewNotificationRequestV21 n = newNotification();
    n.getRecipients().get(0).setPhysicalAddress(null);

    // WHEN
    Set<ConstraintViolation<NewNotificationRequestV21>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // THEN
    Assertions.assertEquals(0, errors.size());
  }



  @Test
    // doesn't pass mvp checks
  void newNotificationRequestForMVPInvalid() {

    // GIVEN
    NewNotificationRequestV21 n = newNotification();
    n.setSenderDenomination(null);
    n.addRecipientsItem(
            NotificationRecipientV21.builder().recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
                    .taxId("FiscalCode").denomination("Nome Cognome / Ragione Sociale")
                    .digitalDomicile(NotificationDigitalAddress.builder().build()).build());
    String noticeCode = n.getRecipients().get(0).getPayments().get(0).getPagoPa().getNoticeCode();

    // WHEN
    Set<ConstraintViolation<NewNotificationRequestV21>> errors;
    errors = validator.checkNewNotificationRequestForMVP(n);

    // THEN
    Assertions.assertEquals(1, errors.size());

    assertConstraintViolationPresentByMessage(errors, "Max one recipient");

  }

  @Test
    // doesn't pass mvp checks
  void newNotificationRequestForMVP() {

    // GIVEN
    NewNotificationRequestV21 n = newNotification();

    // WHEN
    Set<ConstraintViolation<NewNotificationRequestV21>> errors;
    errors = validator.checkNewNotificationRequestForMVP(n);

    // THEN
    Assertions.assertNotNull(errors);
    assertConstraintViolationPresentByMessage(errors, "No recipient payment");
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

  private NewNotificationRequestV21 newNotification() {
    List<NotificationRecipientV21> recipients = new ArrayList<>();
    recipients.add(
            NotificationRecipientV21.builder().recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
                    .taxId("FiscalCode").denomination("Nome Cognome / Ragione Sociale")
                    .digitalDomicile(NotificationDigitalAddress.builder()
                            .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
                    .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo").zip("83100")
                            .province("province").municipality("municipality").at("at").build())
                    .payments(List.of(NotificationPaymentItem.builder().build()))
                    .build());
    return NewNotificationRequestV21.builder().senderDenomination("Sender Denomination")
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(recipients).build();
  }

  private FullSentNotificationV21 newFullSentNotification() {
    return FullSentNotificationV21.builder().sentAt(OffsetDateTime.now()).iun(IUN)
            .paProtocolNumber("protocol1").group("group_1").idempotenceToken("idempotenceToken")
            .timeline(Collections.singletonList(TimelineElementV20.builder().build()))
            .notificationStatus(NotificationStatus.ACCEPTED)
            .documents(Collections.singletonList(NotificationDocument.builder()
                    .contentType("application/pdf")
                    .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                            .build())
                    .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build()))
            .recipients(Collections.singletonList(NotificationRecipientV21.builder()
                    .taxId("LVLDAA85T50G702B").recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
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
                    FullSentNotificationV21.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
            .build();
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

}

