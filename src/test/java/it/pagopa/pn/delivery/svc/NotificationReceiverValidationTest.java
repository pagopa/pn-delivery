package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.commons.configs.IsMVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.apache.commons.codec.digest.DigestUtils;
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
    private IsMVPParameterConsumer isMVPParameterConsumer;

    private static final String IUN = "FAKE-FAKE-FAKE-202209-F-1";
    public static final String ATTACHMENT_BODY_STR = "Body";
    public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
    public static final String VERSION_TOKEN = "version_token";
    public static final String KEY = "key";
    public static final String INVALID_ABSTRACT = "invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars, invalid abstract string length more than max available: 1024 chars";
    public static final String INVALID_SUBJECT = "invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars, invalid abstract string length more than max available: 512 chars";


    private NotificationReceiverValidator validator;

    @BeforeEach
    void initializeValidator() {
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = new NotificationReceiverValidator( factory.getValidator(), cfg, isMVPParameterConsumer);
    }

    @Test
    void invalidEmptyNotification() {

        // GIVEN
        InternalNotification n = new InternalNotification(FullSentNotification.builder().build(), Collections.emptyMap(), Collections.emptyList());

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN

        assertConstraintViolationPresentByField( errors, "recipients" );
        assertConstraintViolationPresentByField( errors, "timeline" );
        assertConstraintViolationPresentByField( errors, "notificationStatusHistory" );
        assertConstraintViolationPresentByField( errors, "documents" );
        assertConstraintViolationPresentByField( errors, "iun" );
        assertConstraintViolationPresentByField( errors, "notificationStatus" );
        assertConstraintViolationPresentByField( errors, "sentAt" );
        assertConstraintViolationPresentByField( errors, "paProtocolNumber" );
        assertConstraintViolationPresentByField( errors, "physicalCommunicationType" );
        assertConstraintViolationPresentByField( errors, "subject" );
        assertConstraintViolationPresentByField( errors, "notificationFeePolicy" );
        assertConstraintViolationPresentByField( errors, "senderDenomination" );
        assertConstraintViolationPresentByField( errors, "senderTaxId" );
        Assertions.assertEquals( 13, errors.size() );
    }

    @Test
    void checkThrowMechanism() {

        // GIVEN
        InternalNotification n = new InternalNotification(FullSentNotification.builder().build(), Collections.emptyMap(), Collections.emptyList());

        // WHEN
        Executable todo = () -> validator.checkNewNotificationBeforeInsertAndThrow(n);

        // THEN
        PnValidationException validationException;
        validationException = Assertions.assertThrows(PnValidationException.class, todo );

        @NotNull @Valid @Size(min = 1) List<ProblemError> errors = validationException.getProblem().getErrors();
        assertProblemErrorConstraintViolationPresentByField( errors, "recipients" );
        assertProblemErrorConstraintViolationPresentByField( errors, "timeline" );
        assertProblemErrorConstraintViolationPresentByField( errors, "notificationStatusHistory" );
        assertProblemErrorConstraintViolationPresentByField( errors, "documents" );
        assertProblemErrorConstraintViolationPresentByField( errors, "iun" );
        assertProblemErrorConstraintViolationPresentByField( errors, "notificationStatus" );
        assertProblemErrorConstraintViolationPresentByField( errors, "sentAt" );
        assertProblemErrorConstraintViolationPresentByField( errors, "paProtocolNumber" );
        assertProblemErrorConstraintViolationPresentByField( errors, "physicalCommunicationType" );
        assertProblemErrorConstraintViolationPresentByField( errors, "subject" );
        assertProblemErrorConstraintViolationPresentByField( errors, "notificationFeePolicy" );
        assertProblemErrorConstraintViolationPresentByField( errors, "senderDenomination" );
        assertProblemErrorConstraintViolationPresentByField( errors, "senderTaxId" );
        Assertions.assertEquals( 13, errors.size() );
    }

    @Test
    void invalidRecipient() {

        // GIVEN
        InternalNotification n = new InternalNotification( notificationWithPhysicalCommunicationType()
                .senderTaxId( "01199250158" )
                .recipients( Collections.singletonList( NotificationRecipient.builder().build() ) ), Collections.emptyMap(), Collections.emptyList() );

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "recipients[0].recipientType" );
        assertConstraintViolationPresentByField( errors, "recipients[0].taxId" );
        assertConstraintViolationPresentByField( errors, "recipients[0].denomination" );
        assertConstraintViolationPresentByField( errors, "notificationFeePolicy" );
        Assertions.assertEquals( 4, errors.size() );
    }

    @Test
    void validRecipientTaxIdOmocode() {
        // GIVEN
        InternalNotification n = new InternalNotification( validDocumentWithPayments()
                .senderTaxId( "01199250158" )
                .recipients( Collections.singletonList( NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        // C.F. Omocodice 0=L 1=M 2=N 3=P 4=Q 5=R 6=S 7=T 8=U 9=V
                        .taxId( "MRNLCU00A01H50MJ" )
                        .denomination( "valid Denomination" )
                        .build() ) ), Collections.emptyMap(), Collections.emptyList() );

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        Assertions.assertEquals( 0, errors.size() );
    }

    @Test
    void invalidRecipientTaxId() {

        // GIVEN
        InternalNotification n = new InternalNotification( notificationWithPhysicalCommunicationType()
                .senderTaxId( "01199250158" )
                .recipients( Collections.singletonList( NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .taxId( "invalidTaxId" )
                        .denomination( "valid Denomination" )
                        .build() ) ), Collections.emptyMap(), Collections.emptyList() );

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "recipients[0].taxId" );
        assertConstraintViolationPresentByField( errors, "notificationFeePolicy" );
        Assertions.assertEquals( 2, errors.size() );
    }

    @Test
    void invalidSenderTaxId() {
        // GIVEN
        InternalNotification n = new InternalNotification( notificationWithPhysicalCommunicationType()
                .senderTaxId( "invalidSenderTaxId" )
                .senderDenomination( "Valid Sender Denomination" )
                , Collections.emptyMap(), Collections.emptyList() );

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByFieldWithExpected( errors, "senderTaxId", 2 );
        assertConstraintViolationPresentByField( errors, "notificationFeePolicy" );
        Assertions.assertEquals( 3, errors.size() );
    }

    @Test
    void invalidAbstract() {
        // GIVEN
        InternalNotification n = new InternalNotification( notificationWithPhysicalCommunicationType()
                ._abstract( INVALID_ABSTRACT )
                .notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.FLAT_RATE )
                , Collections.emptyMap(), Collections.emptyList() );

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "abstract" );
        Assertions.assertEquals( 1, errors.size() );
    }

    @Test
    void invalidSubject() {
        // GIVEN
        InternalNotification n = new InternalNotification( notificationWithPhysicalCommunicationType()
                .subject( INVALID_SUBJECT )
                .notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.FLAT_RATE )
                , Collections.emptyMap(), Collections.emptyList() );

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "subject" );
        Assertions.assertEquals( 1, errors.size() );
    }

    @Test
    void duplicatedRecipientTaxId() {
        // Given
        NewNotificationRequest n = newNotification();
        n.addRecipientsItem( NotificationRecipient.builder()
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .taxId( "FiscalCode" )
                .denomination( "recipientDenomination" )
                .build() );

        // When
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationRequestBeforeInsert( n );

        // Then
        assertConstraintViolationPresentByMessage( errors, "Duplicated recipient taxId" );
    }

    @Test @Disabled
    void invalidNullValuesInCollections() {

        // GIVEN
        InternalNotification n = new InternalNotification( notificationWithPhysicalCommunicationType()
                .recipients( Collections.singletonList( null ) )
                .documents( Collections.singletonList( null ) ), Collections.emptyMap(), Collections.emptyList());

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0]" );
        assertConstraintViolationPresentByField( errors, "recipients[0]" );
        Assertions.assertEquals( 2, errors.size() );
    }

    @Test
    void invalidDocumentAndRecipientWithEmptyFields() {

        // GIVEN
        InternalNotification n = new InternalNotification( notificationWithPhysicalCommunicationType()
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .build())
                )
                .documents( Collections.singletonList(NotificationDocument.builder().build()  ) ), Collections.emptyMap(), Collections.emptyList());
        n.notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.DELIVERY_MODE );

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0].digests" );
        assertConstraintViolationPresentByField( errors, "documents[0].ref" );
        assertConstraintViolationPresentByField( errors, "documents[0].contentType" );
        assertConstraintViolationPresentByField( errors, "recipients[0].recipientType" );
        assertConstraintViolationPresentByField( errors, "recipients[0].taxId" );
        assertConstraintViolationPresentByField( errors, "recipients[0].denomination" );
        Assertions.assertEquals( 6, errors.size() );
    }

    @Test
    void invalidDocumentAndRecipientWithEmptyDigestsAndDigitalDomicile() {

        // GIVEN
        InternalNotification n = new InternalNotification( notificationWithPhysicalCommunicationType()
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .taxId("LVLDAA85T50G702B")
                        .denomination("Ada Lovelace")
                        .digitalDomicile(  NotificationDigitalAddress.builder().build() )
                        .build() )
                )
                .documents( Collections.singletonList(NotificationDocument.builder()
                        //.body( BASE64_BODY )
                        .contentType("Content/Type")
                        .digests( NotificationAttachmentDigests.builder().build() )
                        .build( ))
                ), Collections.emptyMap(), Collections.emptyList());

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0].digests.sha256" );
        assertConstraintViolationPresentByField( errors, "documents[0].ref" );
        assertConstraintViolationPresentByField( errors, "recipients[0].recipientType" );
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.address" );
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.type" );
        assertConstraintViolationPresentByField( errors, "notificationFeePolicy" );
        Assertions.assertEquals( 6, errors.size() );
    }


    @Test
    void validDocumentAndRecipientWithoutPayments() {
        // GIVEN
        InternalNotification n = validDocumentWithoutPayments() ;
        n.notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.DELIVERY_MODE );

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        Assertions.assertEquals( 0, errors.size() );
    }

    @Test
    void validDocumentAndRecipientWitPayments() {

        // GIVEN
        InternalNotification n = validDocumentWithPayments();

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        Assertions.assertEquals( 0, errors.size() );
    }

    @Test
    void invalidPecAddress() {

        // GIVEN
        InternalNotification n = validDocumentWithoutPayments();
        n.notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.DELIVERY_MODE );
        InternalNotification wrongEmail = new InternalNotification( n
                .recipients( Collections.singletonList( n.getRecipients().get(0)
                        .digitalDomicile( n.getRecipients().get(0).getDigitalDomicile()
                                .address( null )
                        ))), Collections.emptyMap(), Collections.emptyList());

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( wrongEmail );

        // THEN
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.address" );
        Assertions.assertEquals( 1, errors.size() );
    }


    @Test
    void testCheckNotificationDocumentFail() {

        InternalNotification notification = new InternalNotification( validDocumentWithoutPayments()
                .documents( Collections.singletonList( NotificationDocument.builder().build() )), Collections.emptyMap(), Collections.emptyList());
        notification.notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.DELIVERY_MODE );

        // WHEN
        Set<ConstraintViolation<InternalNotification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( notification );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0].digests" );
        assertConstraintViolationPresentByField( errors, "documents[0].ref" );
        assertConstraintViolationPresentByField( errors, "documents[0].contentType" );
        Assertions.assertEquals( 3, errors.size() );
    }

    @Test
    // pass all mvp checks
    void newNotificationRequestForMVPValidCheckAddress() {

        Mockito.when( cfg.isNotificationCheckAddress() ).thenReturn( true );

        // GIVEN
        NewNotificationRequest n = newNotification();

        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationRequestForMVP( n );

        // THEN
        Assertions.assertEquals( 0, errors.size() );
    }

    @Test
        // pass all mvp checks
    void newNotificationRequestForMVPValidDontCheckAddress() {

        Mockito.when( cfg.isNotificationCheckAddress() ).thenReturn( false );

        // GIVEN
        NewNotificationRequest n = newNotification();
        n.getRecipients().get(0).setPhysicalAddress(null);

        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationRequestForMVP( n );

        // THEN
        Assertions.assertEquals( 0, errors.size() );
    }

    @Test
    // doesn't pass mvp checks
    void newNotificationRequestForMVPInvalid() {

        Mockito.when( cfg.isNotificationCheckAddress() ).thenReturn( true );

        // GIVEN
        NewNotificationRequest n = newNotification();
        n.setSenderDenomination(null);
        n.addRecipientsItem(NotificationRecipient.builder()
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .taxId("FiscalCode")
                .denomination("Nome Cognome / Ragione Sociale")
                .digitalDomicile(  NotificationDigitalAddress.builder().build() )
                .build());
        n.getRecipients().get(0).setPhysicalAddress(null);
        String noticeCode = n.getRecipients().get(0).getPayment().getNoticeCode();
        n.getRecipients().get(0).getPayment().setNoticeCodeAlternative(noticeCode);

        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationRequestForMVP( n );

        // THEN
        Assertions.assertEquals( 4, errors.size() );
        assertConstraintViolationPresentByMessage( errors, "No sender denomination" );
        assertConstraintViolationPresentByMessage( errors, "Max one recipient" );
        assertConstraintViolationPresentByMessage( errors, "No recipient physical address" );
        assertConstraintViolationPresentByMessage( errors, "Alternative notice code equals to notice code" );
    }

    @Test
    // doesn't pass mvp checks
    void newNotificationRequestForMVPInvalidNoPayment() {
        Mockito.when( cfg.isNotificationCheckAddress() ).thenReturn( true );

        // GIVEN
        NewNotificationRequest n = newNotification();
        n.getRecipients().get(0).setPayment( null );

        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationRequestForMVP( n );

        // THEN
        Assertions.assertNotNull( errors );
        assertConstraintViolationPresentByMessage( errors, "No recipient payment" );
    }

    private <T> void assertConstraintViolationPresentByMessage( Set<ConstraintViolation<T>> set, String message ) {
        long actual = set.stream()
                .filter( cv -> cv.getMessage().equals( message ) )
                .count();
        Assertions.assertEquals( 1, actual);
    }

    private <T> void assertConstraintViolationPresentByField( Set<ConstraintViolation<T>> set, String propertyPath ) {
        long actual = set.stream()
                .filter( cv -> propertyPathToString( cv.getPropertyPath() ).equals( propertyPath ) )
                .count();
        Assertions.assertEquals( 1, actual, "expected validation errors on " + propertyPath );
    }

    private <T> void assertConstraintViolationPresentByFieldWithExpected( Set<ConstraintViolation<T>> set, String propertyPath, long expected ) {
        long actual = set.stream()
                .filter( cv -> propertyPathToString( cv.getPropertyPath() ).equals( propertyPath ) )
                .count();
        Assertions.assertEquals( expected, actual, "expected validation errors on " + propertyPath );
    }


    private void assertProblemErrorConstraintViolationPresentByField(List<ProblemError> set, String propertyPath ) {
        long actual = set.stream()
                .filter( cv -> cv.getElement() != null && cv.getElement().equals( propertyPath ) )
                .count();
        Assertions.assertEquals( 1, actual, "expected validation errors on " + propertyPath );
    }

    private static String propertyPathToString( Path propertyPath ) {
        return propertyPath.toString().replaceFirst(".<[^>]*>$", "");
    }

    private NewNotificationRequest newNotification() {
        List<NotificationRecipient> recipients = new ArrayList<>();
        recipients.add(NotificationRecipient.builder()
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .taxId( "FiscalCode" )
                .denomination( "Nome Cognome / Ragione Sociale" )
                .digitalDomicile( NotificationDigitalAddress.builder()
                        .type( NotificationDigitalAddress.TypeEnum.PEC )
                        .address( "account@domain.it" )
                        .build()
                )
                .physicalAddress(NotificationPhysicalAddress.builder()
                        .address("Indirizzo")
                        .zip("zip")
                        .province("province")
                        .municipality("municipality")
                        .at("at")
                        .build())
                .payment(NotificationPaymentInfo.builder()
                        .noticeCode("noticeCode")
                        .noticeCodeAlternative("noticeCodeAlternative")
                        .build())
                .build());
        return NewNotificationRequest.builder()
                .senderDenomination("Sender Denomination")
                .idempotenceToken("IUN_01")
                .paProtocolNumber( "protocol1" )
                .subject( "subject" )
                .senderTaxId( "paId" )
                .recipients( recipients )
                .build();
    }

    private FullSentNotification newFullSentNotification() {
        return FullSentNotification.builder()
                .sentAt( OffsetDateTime.now() )
                .iun( IUN )
                .paProtocolNumber("protocol1")
                .group("group_1")
                .idempotenceToken("idempotenceToken")
                .timeline( Collections.singletonList( TimelineElement.builder().build() ) )
                .notificationStatus( NotificationStatus.ACCEPTED )
                .documents( Collections.singletonList( NotificationDocument.builder()
                                .contentType( "application/pdf" )
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key( KEY )
                                        .versionToken( VERSION_TOKEN )
                                        .build() )
                                .digests( NotificationAttachmentDigests.builder()
                                        .sha256( SHA256_BODY )
                                        .build() )
                        .build() ) )
                .recipients(Collections.singletonList( NotificationRecipient.builder()
                                .taxId( "LVLDAA85T50G702B" )
                                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                                .denomination( "Ada Lovelace" )
                                .digitalDomicile( NotificationDigitalAddress.builder()
                                        .address( "indirizzo@pec.it" )
                                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                                        .build() )
                        .build() ))
                .notificationStatusHistory( Collections.singletonList( NotificationStatusHistoryElement.builder()
                                .activeFrom( OffsetDateTime.now() )
                                .status( NotificationStatus.ACCEPTED )
                                .relatedTimelineElements( Collections.emptyList() )
                        .build() ) )
                .senderDenomination("Comune di Milano")
                .senderTaxId("01199250158")
                .subject("subject")
                .physicalCommunicationType(FullSentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
                .build();
    }

    private InternalNotification newInternalNotification() {
        return new InternalNotification( newFullSentNotification(), Collections.emptyMap(), Collections.emptyList() );
    }

    private InternalNotification notificationWithPhysicalCommunicationType() {
        return new InternalNotification( newFullSentNotification()
                .physicalCommunicationType( FullSentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 ), Collections.emptyMap(), Collections.emptyList());
    }

    private InternalNotification validDocumentWithoutPayments() {
        return new InternalNotification( newFullSentNotification()
                .documents( Collections.singletonList( NotificationDocument.builder()
                        .contentType( "application/pdf" )
                        .ref( NotificationAttachmentBodyRef.builder()
                                .key( KEY )
                                .versionToken( VERSION_TOKEN )
                                .build() )
                        .digests( NotificationAttachmentDigests.builder()
                                .sha256( SHA256_BODY )
                                .build() )
                        .build() ) ), Collections.emptyMap(), Collections.emptyList());
    }

    private InternalNotification validDocumentWithPayments() {
    	return new InternalNotification( newFullSentNotification()
                .notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.DELIVERY_MODE )
                .recipients(Collections.singletonList( NotificationRecipient.builder()
                        .taxId( "LVLDAA85T50G702B" )
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .denomination( "Ada Lovelace" )
                        .digitalDomicile( NotificationDigitalAddress.builder()
                                .address( "indirizzo@pec.it" )
                                .type(NotificationDigitalAddress.TypeEnum.PEC)
                                .build())
                                .payment( NotificationPaymentInfo.builder()
                                        .f24flatRate( NotificationPaymentAttachment.builder()
                                                .ref( NotificationAttachmentBodyRef.builder()
                                                        .versionToken( VERSION_TOKEN )
                                                        .key( KEY )
                                                        .build() )
                                                .contentType( "application/pdf" )
                                                .digests( NotificationAttachmentDigests.builder()
                                                        .sha256( SHA256_BODY )
                                                        .build() )
                                                .build() )
                                        .creditorTaxId( "12345678901" )
                                        .noticeCode("123456789012345678")
                                        .pagoPaForm( NotificationPaymentAttachment.builder()
                                                .digests( NotificationAttachmentDigests.builder()
                                                        .sha256( SHA256_BODY )
                                                        .build() )
                                                .build()
                                                .contentType( "application/pdf" )
                                                .ref( NotificationAttachmentBodyRef.builder()
                                                        .key( KEY )
                                                        .versionToken( VERSION_TOKEN )
                                                        .build() ))
                                        .build() )
                        .build() )), Collections.emptyMap(), Collections.emptyList());
    }

}

