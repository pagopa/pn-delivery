package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.util.Base64Utils;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

class NotificationReceiverValidationTest {

    public static final String ATTACHMENT_BODY_STR = "Body";
    public static final String BASE64_BODY = Base64Utils.encodeToString(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8));
    public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
    public static final String VERSION_TOKEN = "version_token";
    public static final String KEY = "key";
    public static final NotificationAttachment NOTIFICATION_ATTACHMENT = NotificationAttachment.builder()
            //.body(BASE64_BODY)
            .contentType("Content/Type")
            .digests(NotificationAttachmentDigests.builder()
                    .sha256(SHA256_BODY)
                    .build()
            )
            .build();


    private NotificationReceiverValidator validator;

    @BeforeEach
    void initializeValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = new NotificationReceiverValidator( factory.getValidator() );
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
        Assertions.assertEquals( 11, errors.size() );
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

        Set<ConstraintViolation> errors = validationException.getValidationErrors();
        Set<ConstraintViolation<Object>> errorsCast = (Set<ConstraintViolation<Object>>) ((Object) errors);
        assertConstraintViolationPresentByField( errorsCast, "recipients" );
        assertConstraintViolationPresentByField( errorsCast, "timeline" );
        assertConstraintViolationPresentByField( errorsCast, "notificationStatusHistory" );
        assertConstraintViolationPresentByField( errorsCast, "documents" );
        assertConstraintViolationPresentByField( errorsCast, "iun" );
        assertConstraintViolationPresentByField( errorsCast, "notificationStatus" );
        assertConstraintViolationPresentByField( errorsCast, "sentAt" );
        assertConstraintViolationPresentByField( errorsCast, "paProtocolNumber" );
        assertConstraintViolationPresentByField( errorsCast, "physicalCommunicationType" );
        assertConstraintViolationPresentByField( errorsCast, "subject" );
        assertConstraintViolationPresentByField( errorsCast, "notificationFeePolicy" );
        Assertions.assertEquals( 11, errors.size() );
    }

    @Test
    void invalidRecipient() {

        // GIVEN
        InternalNotification n = new InternalNotification( notificationWithPhysicalCommunicationType()
                .senderTaxId( "taxid" )
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
                        .taxId("FiscalCode")
                        .denomination("Nome Cognome / Ragione Sociale")
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

    private <T> void assertConstraintViolationPresentByField( Set<ConstraintViolation<T>> set, String propertyPath ) {
        long actual = set.stream()
                .filter( cv -> propertyPathToString( cv.getPropertyPath() ).equals( propertyPath ) )
                .count();
        Assertions.assertEquals( 1, actual, "expected validation errors on " + propertyPath );
    }

    private static String propertyPathToString( Path propertyPath ) {
        return propertyPath.toString().replaceFirst(".<[^>]*>$", "");
    }

    private NewNotificationRequest newNotification() {
        return NewNotificationRequest.builder()
                .idempotenceToken("IUN_01")
                .paProtocolNumber( "protocol1" )
                .subject( "subject" )
                .senderTaxId( "paId" )
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .taxId( "FiscalCode" )
                        .denomination( "Nome Cognome / Ragione Sociale" )
                        .digitalDomicile( NotificationDigitalAddress.builder()
                                .type( NotificationDigitalAddress.TypeEnum.PEC )
                                .address( "account@domain.it" )
                                .build()
                        )
                        .build())
                )
                .build();
    }

    private FullSentNotification newFullSentNotification() {
        return FullSentNotification.builder()
                .sentAt(Date.from(Instant.now()))
                .iun( "IUN_01" )
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
                                .taxId( "recipientTaxId" )
                                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                                .denomination( "recipientDenomination" )
                                .digitalDomicile( NotificationDigitalAddress.builder()
                                        .address( "indirizzo@pec.it" )
                                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                                        .build() )
                        .build() ))
                .notificationStatusHistory( Collections.singletonList( NotificationStatusHistoryElement.builder()
                                .activeFrom( Date.from( Instant.now() ) )
                                .status( NotificationStatus.ACCEPTED )
                                .relatedTimelineElements( Collections.emptyList() )
                        .build() ) )
                .senderDenomination("senderDenomination")
                .senderTaxId("senderTaxId")
                .subject("subject")
                .physicalCommunicationType(FullSentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
                .build();
    }

    private InternalNotification newNotificationRequets() {
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
                        .taxId( "recipientTaxId" )
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .denomination( "recipientDenomination" )
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
                                        .noticeNumber("123456789012345678")
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

