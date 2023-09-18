package it.pagopa.pn.delivery.svc;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.models.InternalNotification;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.AdditionalMatchers.and;
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
  public static final String INVALID_ABSTRACT =
      "invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars";
  public static final String INVALID_SUBJECT =
      "invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars";
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
  void invalidEmptyNotification() {

    // GIVEN
    InternalNotification n =
        new InternalNotification(FullSentNotificationV20.builder().build());

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(n);

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

    // GIVEN
    InternalNotification n =
        new InternalNotification(FullSentNotificationV20.builder().build());

    // WHEN
    Executable todo = () -> validator.checkNewNotificationBeforeInsertAndThrow(n);

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
    NewNotificationRequest n = newNotification();
    n.addRecipientsItem(
            NotificationRecipient.builder().recipientType(NotificationRecipient.RecipientTypeEnum.PG)
                    .taxId("1234c56").denomination("recipientDenomination").build());

    // When
    Set<ConstraintViolation<NewNotificationRequest>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // Then
    assertConstraintViolationPresentByMessage(errors, "SEND accepts only numerical taxId for PG recipient 1");
  }

  @Test
  void invalidRecipient() {

    // GIVEN
    InternalNotification n = new InternalNotification(
        notificationWithPhysicalCommunicationType().senderTaxId("01199250158").sourceChannel(X_PAGOPA_PN_SRC_CH)
            .recipients(Collections.singletonList(NotificationRecipient.builder().build())));

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(n);

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
    InternalNotification n = new InternalNotification(
        validDocumentWithPayments().senderTaxId("01199250158")
                .sourceChannel(X_PAGOPA_PN_SRC_CH)
            .recipients(Collections.singletonList(NotificationRecipient.builder()
                .recipientType(NotificationRecipient.RecipientTypeEnum.PF)
                // C.F. Omocodice 0=L 1=M 2=N 3=P 4=Q 5=R 6=S 7=T 8=U 9=V
                .taxId("MRNLCU00A01H50MJ")
                    .denomination("valid Denomination")
                    .physicalAddress( createPhysicalAddress() )
                    .build())));

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(n);

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

    // GIVEN
    InternalNotification n = new InternalNotification(
        notificationWithPhysicalCommunicationType().senderTaxId("01199250158")
                .sourceChannel(X_PAGOPA_PN_SRC_CH)
            .recipients(Collections.singletonList(NotificationRecipient.builder()
                .recipientType(NotificationRecipient.RecipientTypeEnum.PF).taxId("invalidTaxId")
                .denomination("valid Denomination")
                    .physicalAddress( createPhysicalAddress() )
                    .build()
            )));

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByField(errors, "recipients[0].taxId");
    assertConstraintViolationPresentByField(errors, "notificationFeePolicy");
    Assertions.assertEquals(2, errors.size());
  }

  @Test
  void invalidSenderTaxId() {
    // GIVEN
    InternalNotification n = new InternalNotification(notificationWithPhysicalCommunicationType()
        .senderTaxId("invalidSenderTaxId")
            .senderDenomination("Valid Sender Denomination")
            .sourceChannel(X_PAGOPA_PN_SRC_CH));

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByFieldWithExpected(errors, "senderTaxId", 2);
    assertConstraintViolationPresentByField(errors, "notificationFeePolicy");
    Assertions.assertEquals(3, errors.size());
  }

  @Test
  void invalidAbstract() {
    // GIVEN
    InternalNotification n = new InternalNotification(
        notificationWithPhysicalCommunicationType()._abstract(INVALID_ABSTRACT)
            .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE));

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByField(errors, "abstract"); // this is a validator too, that checks what props have errors in constructor (at the moment are 2 more than the normal(3) but expect1)
    Assertions.assertEquals(1, errors.size());
  }

  @Test
  void invalidSubject() {
    // GIVEN
    InternalNotification n =
        new InternalNotification(
            notificationWithPhysicalCommunicationType().subject(INVALID_SUBJECT)
                .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE));

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByField(errors, "subject");
    Assertions.assertEquals(1, errors.size());
  }

  @Test
  void duplicatedRecipientTaxId() {
    // Given
    NewNotificationRequest n = newNotification();
    n.addRecipientsItem(
        NotificationRecipient.builder().recipientType(NotificationRecipient.RecipientTypeEnum.PF)
            .taxId("FiscalCode").denomination("recipientDenomination").build());

    // When
    Set<ConstraintViolation<NewNotificationRequest>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // Then
    assertConstraintViolationPresentByMessage(errors, "Duplicated recipient taxId");
  }


  @Test
  @Disabled("Documents field required in NewNotificationRequest")
  void invalidNullValuesInCollections() {

    // GIVEN
    InternalNotification n = new InternalNotification(notificationWithPhysicalCommunicationType()
        .recipients(Collections.singletonList(null)).documents(Collections.singletonList(null)));

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByField(errors, "documents[0]");
    assertConstraintViolationPresentByField(errors, "recipients[0]");
    Assertions.assertEquals(2, errors.size());
  }

  @Test
  void invalidDocumentAndRecipientWithEmptyFields() {

    // GIVEN
    InternalNotification n = new InternalNotification(
        notificationWithPhysicalCommunicationType()
            .recipients(Collections.singletonList(NotificationRecipient.builder().build()))
            .documents(Collections.singletonList(NotificationDocument.builder().build())));
    n.notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(n);

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
    InternalNotification n =
        new InternalNotification(
            notificationWithPhysicalCommunicationType()
                .recipients(Collections.singletonList(NotificationRecipient
                    .builder().taxId("LVLDAA85T50G702B").denomination("Ada Lovelace")
                    .digitalDomicile(NotificationDigitalAddress.builder().build())
                        .physicalAddress( createPhysicalAddress() )
                        .build()))
                .documents(Collections.singletonList(NotificationDocument.builder()
                    // .body( BASE64_BODY )
                    .contentType("application/pdf")
                    .digests(NotificationAttachmentDigests.builder().build()).build())));

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(n);

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

    // GIVEN
    InternalNotification n = validDocumentWithoutPayments();
    n.notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);
    InternalNotification wrongEmail = new InternalNotification(
        n.recipients(Collections.singletonList(n.getRecipients().get(0)
            .digitalDomicile(n.getRecipients().get(0).getDigitalDomicile().address(null)))));

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(wrongEmail);

    // THEN
    assertConstraintViolationPresentByField(errors, "recipients[0].digitalDomicile.address");
    Assertions.assertEquals(1, errors.size());
  }


  @Test
  void testCheckNotificationDocumentFail() {

    InternalNotification notification = new InternalNotification(
        validDocumentWithoutPayments()
            .documents(Collections.singletonList(NotificationDocument.builder().build())));
    notification
        .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);

    // WHEN
    Set<ConstraintViolation<InternalNotification>> errors;
    errors = validator.checkNewNotificationBeforeInsert(notification);

    // THEN
    assertConstraintViolationPresentByField(errors, "documents[0].digests");
    assertConstraintViolationPresentByField(errors, "documents[0].ref");
    assertConstraintViolationPresentByField(errors, "documents[0].contentType");
    Assertions.assertEquals(3, errors.size());
  }

  @Test
  void newNotificationRequestWhitInvalidPhysicalAddress() {
    // GIVEN
    NewNotificationRequest n = newNotification();
    n.getRecipients().get(0).setPhysicalAddress(NotificationPhysicalAddress.builder()
                    .municipality( "municipality" )
                    .address( "address" )
                    .build()
    );

    // WHEN
    Set<ConstraintViolation<NewNotificationRequest>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByMessage(errors, "No province provided in physical address");
  }

  @Test
  void newNotificationRequestWhitInvalidPhysicalAddressForeignStateItaly() {
    // GIVEN
    NewNotificationRequest n = newNotification();
    n.getRecipients().get(0).setPhysicalAddress(NotificationPhysicalAddress.builder()
            .foreignState("Italia")
            .municipality( "municipality" )
            .address( "address" )
            .build()
    );

    // WHEN
    Set<ConstraintViolation<NewNotificationRequest>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // THEN
    assertConstraintViolationPresentByMessage(errors, "No province provided in physical address");
  }

  @Test
  @Disabled("Since PN-2401")
  // pass all mvp checks
  void newNotificationRequestForValidDontCheckAddress() {

    // GIVEN
    NewNotificationRequest n = newNotification();
    n.getRecipients().get(0).setPhysicalAddress(null);

    // WHEN
    Set<ConstraintViolation<NewNotificationRequest>> errors;
    errors = validator.checkNewNotificationRequestBeforeInsert(n);

    // THEN
    Assertions.assertEquals(0, errors.size());
  }



  @Test
  // doesn't pass mvp checks
  void newNotificationRequestForMVPInvalid() {

    // GIVEN
    NewNotificationRequest n = newNotification();
    n.setSenderDenomination(null);
    n.addRecipientsItem(
        NotificationRecipient.builder().recipientType(NotificationRecipient.RecipientTypeEnum.PF)
            .taxId("FiscalCode").denomination("Nome Cognome / Ragione Sociale")
            .digitalDomicile(NotificationDigitalAddress.builder().build()).build());
    String noticeCode = n.getRecipients().get(0).getPayment().getNoticeCode();
    n.getRecipients().get(0).getPayment().setNoticeCodeAlternative(noticeCode);

    // WHEN
    Set<ConstraintViolation<NewNotificationRequest>> errors;
    errors = validator.checkNewNotificationRequestForMVP(n);

    // THEN
    Assertions.assertEquals(1, errors.size()); 

    assertConstraintViolationPresentByMessage(errors, "Max one recipient");

  }

  @Test
  // doesn't pass mvp checks
  void newNotificationRequestForMVP() {

    // GIVEN
    NewNotificationRequest n = newNotification();
    n.getRecipients().get(0).setPayment(null);

    // WHEN
    Set<ConstraintViolation<NewNotificationRequest>> errors;
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
      assertThat(errors, hasSize(2));
      assertThat(errors, hasItems(
              hasProperty("message", Matchers.containsString("denomination")),
              hasProperty("message", Matchers.containsString("exceed"))
      ));
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
    assertThat(errors, hasSize(10));
    assertThat(errors, hasItems(
            hasProperty("message", allOf(Matchers.containsString("denomination"), Matchers.containsString("recipient 0"))),
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

  private NewNotificationRequest newNotification() {
    List<NotificationRecipient> recipients = new ArrayList<>();
    recipients.add(
        NotificationRecipient.builder().recipientType(NotificationRecipient.RecipientTypeEnum.PF)
            .taxId("FiscalCode").denomination("Nome Cognome / Ragione Sociale")
            .digitalDomicile(NotificationDigitalAddress.builder()
                .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
            .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo").zip("83100")
                .province("province").municipality("municipality").at("at").build())
            .payment(NotificationPaymentInfo.builder().noticeCode("noticeCode")
                .noticeCodeAlternative("noticeCodeAlternative").build())
            .build());
    return NewNotificationRequest.builder().senderDenomination("Sender Denomination")
        .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
        .senderTaxId("paId").recipients(recipients).build();
  }

  private FullSentNotificationV20 newFullSentNotification() {
    return FullSentNotificationV20.builder().sentAt(OffsetDateTime.now()).iun(IUN)
        .paProtocolNumber("protocol1").group("group_1").idempotenceToken("idempotenceToken")
        .timeline(Collections.singletonList(TimelineElementV20.builder().build()))
        .notificationStatus(NotificationStatus.ACCEPTED)
        .documents(Collections.singletonList(NotificationDocument.builder()
            .contentType("application/pdf")
            .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                .build())
            .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build()))
        .recipients(Collections.singletonList(NotificationRecipient.builder()
            .taxId("LVLDAA85T50G702B").recipientType(NotificationRecipient.RecipientTypeEnum.PF)
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
                FullSentNotificationV20.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
        .build();
  }

  private NewNotificationRequest badRecipientsNewNotification() {
    List<NotificationRecipient> recipients = new ArrayList<>();
    recipients.add(
            NotificationRecipient.builder().recipientType(NotificationRecipient.RecipientTypeEnum.PF)
                    .taxId("FiscalCode").denomination("Nome Cognome! / Ragione Sociale!")
                    .digitalDomicile(NotificationDigitalAddress.builder()
                            .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
                    .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo").zip("83100")
                            .province("province").municipality("municipalitymorethan40characters").at("at").build())
                    .payment(NotificationPaymentInfo.builder().noticeCode("noticeCode")
                            .noticeCodeAlternative("noticeCodeAlternative").build())
                    .build());
    return NewNotificationRequest.builder().senderDenomination("Sender Denomination")
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(recipients).build();
  }

  private NewNotificationRequest moreBadRecipientsNewNotification() {
    List<NotificationRecipient> recipients = new ArrayList<>();
    recipients.add(NotificationRecipient.builder().recipientType(NotificationRecipient.RecipientTypeEnum.PF)
                    .taxId("FiscalCode").denomination("Nome Cognome! / Ragione Sociale!")
                    .digitalDomicile(NotificationDigitalAddress.builder()
                            .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
                    .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo?").zip("83100*")
                            .province("province_").municipality("municipalitymorethan40characters-").municipalityDetails("municipalityDetails/")
                            .at("at.").addressDetails("addressDetails0").foreignState("foreignState ").build())
                    .payment(NotificationPaymentInfo.builder().noticeCode("noticeCode")
                            .noticeCodeAlternative("noticeCodeAlternative").build())
                    .build());
    recipients.add(NotificationRecipient.builder().recipientType(NotificationRecipient.RecipientTypeEnum.PF)
                    .taxId("FiscalCode").denomination("Nome Cognome / Ragione Sociale")
                    .digitalDomicile(NotificationDigitalAddress.builder()
                            .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
                    .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo").zip("83100")
                    .province("province").municipality("municipality!").municipalityDetails("municipalityDetails?")
                    .at("at_").addressDetails("addressDetails$").foreignState("foreignState%").build())
                    .payment(NotificationPaymentInfo.builder().noticeCode("noticeCode")
                            .noticeCodeAlternative("noticeCodeAlternative").build())
                    .build());
    return NewNotificationRequest.builder().senderDenomination("Sender Denomination")
            .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject_length")
            .senderTaxId("paId").recipients(recipients).build();
  }

  private InternalNotification newInternalNotification() {
    return new InternalNotification(newFullSentNotification());
  }

  private InternalNotification notificationWithPhysicalCommunicationType() {
    return new InternalNotification(
        newFullSentNotification().physicalCommunicationType(
                FullSentNotificationV20.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890));
  }

  private InternalNotification validDocumentWithoutPayments() {
    return new InternalNotification(
        newFullSentNotification().documents(Collections.singletonList(NotificationDocument.builder()
            .contentType("application/pdf")
            .ref(NotificationAttachmentBodyRef.builder().key(KEY).versionToken(VERSION_TOKEN)
                .build())
            .digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build()).build())));
  }

  private InternalNotification validDocumentWithPayments() {
    return new InternalNotification(
        newFullSentNotification()
                .recipientIds(Collections.emptyList())
                .sourceChannel(X_PAGOPA_PN_SRC_CH)
            .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
            .recipients(Collections.singletonList(NotificationRecipient.builder()
                .taxId("LVLDAA85T50G702B").recipientType(NotificationRecipient.RecipientTypeEnum.PF)
                .denomination("Ada Lovelace")
                .digitalDomicile(NotificationDigitalAddress.builder().address("indirizzo@pec.it")
                    .type(NotificationDigitalAddress.TypeEnum.PEC).build())
                .payment(NotificationPaymentInfo.builder()
                    .creditorTaxId("12345678901").noticeCode("123456789012345678")
                    .pagoPaForm(NotificationPaymentAttachment.builder()
                        .digests(
                            NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
                        .build().contentType("application/pdf")
                        .ref(NotificationAttachmentBodyRef.builder().key(KEY)
                            .versionToken(VERSION_TOKEN).build()))
                    .build())
                .physicalAddress( createPhysicalAddress() )
                .build())));
  }

}

