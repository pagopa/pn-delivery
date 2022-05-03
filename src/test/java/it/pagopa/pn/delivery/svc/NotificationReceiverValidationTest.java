package it.pagopa.pn.delivery.svc;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.util.Base64Utils;

import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.svc.NotificationReceiverValidator;

class NotificationReceiverValidationTest {

    public static final String ATTACHMENT_BODY_STR = "Body";
    public static final String BASE64_BODY = Base64Utils.encodeToString(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8));
    public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
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
        NewNotificationRequest n = NewNotificationRequest.builder().build();

        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
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
        NewNotificationRequest n = NewNotificationRequest.builder().build();

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
    	NewNotificationRequest n = notificationWithPhysicalCommunicationType()
                .senderTaxId( "taxid" )
    			.recipients( Collections.emptyList() )
    			.documents( Collections.emptyList() );
    			
        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
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
        NewNotificationRequest n = notificationWithPhysicalCommunicationType()
    			.recipients( Collections.singletonList( null ) )
                .documents( Collections.singletonList( null ) );

        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0]" );
        assertConstraintViolationPresentByField( errors, "recipients[0]" );
        Assertions.assertEquals( 2, errors.size() );
    }

    @Test
    void invalidDocumentAndRecipientWithEmptyFields() {

        // GIVEN    	
        NewNotificationRequest n = notificationWithPhysicalCommunicationType()
    			.recipients( Collections.singletonList(NotificationRecipient.builder()
                      .build())
                )
                .documents( Collections.singletonList(NotificationDocument.builder().build()  ) );

        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0].digests" );
        assertConstraintViolationPresentByField( errors, "documents[0].ref.key" );
        assertConstraintViolationPresentByField( errors, "documents[0].ref.versionToken" );
        assertConstraintViolationPresentByField( errors, "recipients[0].recipientType" );
        assertConstraintViolationPresentByField( errors, "recipients[0].taxId" );
        assertConstraintViolationPresentByField( errors, "recipients[0].denomination" );
        Assertions.assertEquals( 6, errors.size() );
    }

    @Test
    void invalidDocumentAndRecipientWithEmptyDigestsAndDigitalDomicile() {

        // GIVEN
        NewNotificationRequest n = notificationWithPhysicalCommunicationType()
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
    						);
    	    	
        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0].digests.sha256" );
        assertConstraintViolationPresentByField( errors, "recipients[0].recipientType" );
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.address" );
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.type" );
        Assertions.assertEquals( 4, errors.size() );
    }

    @Test
    void invalidPhysicalCommunicationType() {
        // GIVEN
        NewNotificationRequest n = newNotificationRequets();
        NewNotificationRequest n2 = n
        					.documents( Collections.singletonList(NotificationDocument.builder().build() ));
        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n2 );

        // THEN
        assertConstraintViolationPresentByField( errors, "physicalCommunicationType" );
        Assertions.assertEquals( 1, errors.size() );
    }
    
    @Test
    void validDocumentAndRecipientWithoutPayments() {
        // GIVEN
        NewNotificationRequest n = validDocumentWithoutPayments() ;
                
        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        Assertions.assertEquals( 0, errors.size() );
    }

    /*@Test
    void validDocumentAndRecipientWitPayments() {

        // GIVEN
        Notification n = validDocumentWithPayments();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        Assertions.assertEquals( 0, errors.size() );
    }*/

    @Test
    void invalidPecAddress() {

        // GIVEN
        NewNotificationRequest n = validDocumentWithoutPayments();
        NewNotificationRequest wrongEmail = n
                .recipients( Collections.singletonList( n.getRecipients().get(0)
                        .digitalDomicile( n.getRecipients().get(0).getDigitalDomicile()
                                .address("WrongPecAddress")
                        )));

        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationBeforeInsert( wrongEmail );

        // THEN
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.address" );
        Assertions.assertEquals( 1, errors.size() );
    }

    @Test
    void testCheckNotificationAttachmentsBas64Fail() {

        NotificationAttachment attachmentNoBase64Body = NotificationAttachment.builder()
                .contentType("content/Type")
                //.body("MalformedBase64--")
                .build();

        NewNotificationRequest notification = validDocumentWithoutPayments()
                .documents( Collections.singletonList( NotificationDocument.builder().build() ) );

        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
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
                //.body("Body") // body is a valid Base64 string
                .digests( NotificationAttachmentDigests.builder()
                        // - well formed but wrong digest
                        .sha256("1234567890123456789012345678901234567890123456789012345678901234")
                        .build())
                .build();

        NewNotificationRequest notification = validDocumentWithoutPayments()
                .documents( Collections.singletonList( NotificationDocument.builder().build() ));

        // WHEN
        Set<ConstraintViolation<NewNotificationRequest>> errors;
        errors = validator.checkNewNotificationBeforeInsert( notification );

        // THEN
        assertConstraintViolationPresentByField( errors, "documents[0]" );
        assertConstraintViolationPresentByField( errors, "payment.f24.flatRate" );
        assertConstraintViolationPresentByField( errors, "payment.f24.analog" );
        assertConstraintViolationPresentByField( errors, "payment.f24.digital" );
        Assertions.assertEquals( 4, errors.size() );
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

    private NewNotificationRequest newNotificationRequets() {
        return NewNotificationRequest.builder()
                .paProtocolNumber("protocol1")
                .group("group_1")
                .idempotenceToken("idempotenceToken")
                .recipients(Collections.singletonList( NotificationRecipient.builder().build() ))
                .senderDenomination("senderDenomination")
                .senderTaxId("senderTaxId")
                .subject("subject")
                .physicalCommunicationType(NewNotificationRequest.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
                .build();

    }
    
    private NewNotificationRequest notificationWithPhysicalCommunicationType() {
    	return newNotificationRequets()
    			.physicalCommunicationType( NewNotificationRequest.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 );
    }
    
    private NewNotificationRequest validDocumentWithoutPayments() {
    	return notificationWithPhysicalCommunicationType()
    			.documents( Collections.singletonList(NotificationDocument.builder().build()));
    }

    /*private NewNotificationRequest validDocumentWithPayments() {
    	return validDocumentWithoutPayments()
    			.payment( NotificationPaymentInfo.builder()
    						.f24( NotificationPaymentInfo.F24.builder()
    								.analog( NOTIFICATION_ATTACHMENT )
    								.flatRate( NOTIFICATION_ATTACHMENT )
    								.digital( NOTIFICATION_ATTACHMENT )
    								.build() )
    						.build() )
    			.build();
    }*/
   
}
