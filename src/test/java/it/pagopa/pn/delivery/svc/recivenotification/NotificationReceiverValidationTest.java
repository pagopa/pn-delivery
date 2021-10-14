package it.pagopa.pn.delivery.svc.recivenotification;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.util.Base64Utils;

import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationPaymentInfo;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.svc.NotificationReceiverValidator;

class NotificationReceiverValidationTest {

    public static final String ATTACHMENT_BODY_STR = "Body";
    public static final String BASE64_BODY = Base64Utils.encodeToString(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8));
    public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
    public static final NotificationAttachment NOTIFICATION_ATTACHMENT = NotificationAttachment.builder()
            .body(BASE64_BODY)
            .contentType("Content/Type")
            .digests(NotificationAttachment.Digests.builder()
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
        Notification n = Notification.builder().build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents" );
        assertConstraintViolationPresentByField( errors, "sender" );
        assertConstraintViolationPresentByField( errors, "recipients" );
        assertConstraintViolationPresentByField( errors, "paNotificationId" );
        assertConstraintViolationPresentByField( errors, "subject" );
        assertConstraintViolationPresentByField( errors, "physicalCommunicationType" );
        Assertions.assertEquals( 6, errors.size() );
    }

    @Test
    void checkThrowMechanism() {
    	
        // GIVEN
        Notification n = Notification.builder().build();

        // WHEN
        Executable todo = () -> validator.checkNewNotificationBeforeInsertAndThrow(n);

        // THEN
        PnValidationException validationException;
        validationException = Assertions.assertThrows(PnValidationException.class, todo );

        Set<ConstraintViolation> errors = validationException.getValidationErrors();
        Set<ConstraintViolation<Object>> errorsCast = (Set<ConstraintViolation<Object>>) ((Object) errors);
        assertConstraintViolationPresentByField( errorsCast, "documents" );
        assertConstraintViolationPresentByField( errorsCast, "sender" );
        assertConstraintViolationPresentByField( errorsCast, "recipients" );
        assertConstraintViolationPresentByField( errorsCast, "paNotificationId" );
        assertConstraintViolationPresentByField( errorsCast, "subject" );
        assertConstraintViolationPresentByField( errorsCast, "physicalCommunicationType" );
        Assertions.assertEquals( 6, errors.size() );
    }

    @Test
    void invalidEmptyCollections() {
    	
        // GIVEN   	
    	Notification n = notificationWithPhysicalCommunicationType().toBuilder()
    			.sender( NotificationSender.builder().build() )
    			.recipients( Collections.emptyList() )
    			.documents( Collections.emptyList() )
    			.build();
    			
        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "sender.paId" );
        assertConstraintViolationPresentByField( errors, "documents" );
        assertConstraintViolationPresentByField( errors, "recipients" );
        Assertions.assertEquals( 3, errors.size() );
    }

    @Test
    void invalidNullValuesInCollections() {

        // GIVEN        
        Notification n = notificationWithPhysicalCommunicationType().toBuilder()
    			.recipients( Collections.singletonList( null ) )
                .documents( Collections.singletonList( null ) )
    			.build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0]" );
        assertConstraintViolationPresentByField( errors, "recipients[0]" );
        Assertions.assertEquals( 2, errors.size() );
    }

    @Test
    void invalidDocumentAndRecipientWithEmptyFields() {

        // GIVEN    	
        Notification n = notificationWithPhysicalCommunicationType().toBuilder()
    			.recipients( Collections.singletonList(NotificationRecipient.builder()
                      .build())
    			)
    			.documents( Collections.singletonList(NotificationAttachment.builder()
                      .ref( NotificationAttachment.Ref.builder()
                              .build()
                      )
                      .build())
    			)
    			.build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0].digests" );
        assertConstraintViolationPresentByField( errors, "documents[0].ref.key" );
        assertConstraintViolationPresentByField( errors, "documents[0].ref.versionToken" );
        assertConstraintViolationPresentByField( errors, "recipients[0].taxId" );
        assertConstraintViolationPresentByField( errors, "recipients[0].denomination" );
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile" );
        Assertions.assertEquals( 6, errors.size() );
    }

    @Test
    void invalidDocumentAndRecipientWithEmptyDigestsAndDigitalDomicile() {

        // GIVEN
    	Notification n = notificationWithPhysicalCommunicationType().toBuilder()
    						.recipients( Collections.singletonList(NotificationRecipient.builder()
    								.taxId("FiscalCode")
    								.denomination("Nome Cognome / Ragione Sociale")
    								.digitalDomicile( DigitalAddress.builder().build() )
    								.build() )
    						)
    						.documents( Collections.singletonList(NotificationAttachment.builder()
    								.body( BASE64_BODY )
    								.contentType("Content/Type")
    								.digests( NotificationAttachment.Digests.builder().build() )
    								.build( ))
    						)
    						.build();
    	    	
        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0].digests.sha256" );
        //assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.address" );
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.type" );
        Assertions.assertEquals( 2, errors.size() );
    }

    @Test
    void invalidPhysicalCommunicationType() {
        // GIVEN
        Notification n = newNotification();
        Notification n2 = n.toBuilder()
        					.documents( Collections.singletonList( NOTIFICATION_ATTACHMENT ) )
        					.build();
        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n2 );

        // THEN
        assertConstraintViolationPresentByField( errors, "physicalCommunicationType" );
        Assertions.assertEquals( 1, errors.size() );
    }
    
    @Test
    void validDocumentAndRecipientWithoutPayments() {
        // GIVEN
        Notification n = validDocumentWithoutPayments() ;
                
        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        Assertions.assertEquals( 0, errors.size() );
    }

    @Test
    void validDocumentAndRecipientWitPayments() {

        // GIVEN
        Notification n = validDocumentWithPayments();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        Assertions.assertEquals( 0, errors.size() );
    }

    @Test
    void invalidPecAddress() {

        // GIVEN
        Notification n = validDocumentWithoutPayments();
        Notification wrongEmail = n.toBuilder()
                .recipients( Collections.singletonList( n.getRecipients().get(0).toBuilder()
                        .digitalDomicile( n.getRecipients().get(0).getDigitalDomicile().toBuilder()
                                .address("WrongPecAddress")
                                .build()
                        )
                        .build()))
                .build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( wrongEmail );

        // THEN
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.address" );
        Assertions.assertEquals( 1, errors.size() );
    }

    @Test
    void testCheckNotificationAttachmentsBas64Fail() {

        NotificationAttachment attachmentNoBase64Body = NotificationAttachment.builder()
                .contentType("content/Type")
                .body("MalformedBase64--")
                .build();

        Notification notification = validDocumentWithPayments().toBuilder()
                .documents( Collections.singletonList(
                        attachmentNoBase64Body
                ))
                .payment( NotificationPaymentInfo.builder()
                        .f24(NotificationPaymentInfo.F24.builder()
                                .analog( attachmentNoBase64Body )
                                .flatRate( attachmentNoBase64Body )
                                .digital( attachmentNoBase64Body )
                                .build())
                        .build())
                .build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( notification );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0].body" );
        assertConstraintViolationPresentByField( errors, "documents[0].digests" );
        assertConstraintViolationPresentByField( errors, "payment.f24.flatRate.body" );
        assertConstraintViolationPresentByField( errors, "payment.f24.flatRate.digests" );
        assertConstraintViolationPresentByField( errors, "payment.f24.digital.body" );
        assertConstraintViolationPresentByField( errors, "payment.f24.digital.digests" );
        assertConstraintViolationPresentByField( errors, "payment.f24.analog.body" );
        assertConstraintViolationPresentByField( errors, "payment.f24.analog.digests" );
        Assertions.assertEquals( 8, errors.size() );
    }

    @Test
    void testCheckNotificationAttachmentsSha256Fail() {

        NotificationAttachment attachmentWithWrongSha256 = NotificationAttachment.builder()
                .contentType("content/Type")
                .body("Body") // body is a valid Base64 string
                .digests( NotificationAttachment.Digests.builder()
                        // - well formed but wrong digest
                        .sha256("1234567890123456789012345678901234567890123456789012345678901234")
                        .build())
                .build();

        Notification notification = validDocumentWithPayments().toBuilder()
                .documents( Collections.singletonList(
                        attachmentWithWrongSha256
                ))
                .payment( NotificationPaymentInfo.builder()
                        .f24(NotificationPaymentInfo.F24.builder()
                                .analog( attachmentWithWrongSha256 )
                                .flatRate( attachmentWithWrongSha256 )
                                .digital( attachmentWithWrongSha256 )
                                .build())
                        .build())
                .build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( notification );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0]" );
        assertConstraintViolationPresentByField( errors, "payment.f24.flatRate" );
        assertConstraintViolationPresentByField( errors, "payment.f24.analog" );
        assertConstraintViolationPresentByField( errors, "payment.f24.digital" );
        Assertions.assertEquals( 4, errors.size() );
    }

    @Test
    void successAttachmentDigest() {
        validator.checkPreloadedDigests( "attachmentKey",
                NotificationAttachment.Digests.builder().sha256("expected").build(),
                NotificationAttachment.Digests.builder().sha256("expected").build()
            );
    }

    @Test
    void failAttachmentDigest() {
        // Given
        NotificationAttachment.Digests expected = NotificationAttachment.Digests.builder().sha256("expected").build();
        NotificationAttachment.Digests actual = NotificationAttachment.Digests.builder().sha256("wrong").build();
        // When
        PnValidationException exc = Assertions.assertThrows( PnValidationException.class, () ->
                validator.checkPreloadedDigests( "attachmentKey", expected, actual )
            );
        Path propPath = exc.getValidationErrors().iterator().next().getPropertyPath();

        // Then
        Assertions.assertEquals( "attachmentKey", propertyPathToString( propPath ));
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

    private Notification newNotification() {
        return Notification.builder()
        		.iun("IUN_01")
                .paNotificationId( "protocol1" )
                .subject( "subject" )
                .sender(NotificationSender.builder()
                        .paId( "paId" )
                        .build()
                )
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .taxId( "FiscalCode" )
                        .denomination( "Nome Cognome / Ragione Sociale" )
                        .digitalDomicile( DigitalAddress.builder()
                                .type( DigitalAddressType.PEC )
                                .address( "account@domain.it" )
                                .build()
                        )
                        .build())
                )
                .build();
    }
    
    private Notification notificationWithPhysicalCommunicationType() {
    	return newNotification().toBuilder()
    			.physicalCommunicationType( ServiceLevelType.REGISTERED_LETTER_890 )
    			.build();
    }
    
    private Notification validDocumentWithoutPayments() {
    	return notificationWithPhysicalCommunicationType().toBuilder()
    			.documents( Collections.singletonList( NOTIFICATION_ATTACHMENT ) )
    			.build();
    }

    private Notification validDocumentWithPayments() {
    	return validDocumentWithoutPayments().toBuilder()
    			.payment( NotificationPaymentInfo.builder()
    						.f24( NotificationPaymentInfo.F24.builder()
    								.analog( NOTIFICATION_ATTACHMENT )
    								.flatRate( NOTIFICATION_ATTACHMENT )
    								.digital( NOTIFICATION_ATTACHMENT )
    								.build() )
    						.build() )
    			.build();
    }
   
}
