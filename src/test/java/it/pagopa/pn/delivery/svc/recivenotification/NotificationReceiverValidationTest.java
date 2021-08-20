package it.pagopa.pn.delivery.svc.recivenotification;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.common.messages.PnValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.Collections;
import java.util.Set;

class NotificationReceiverValidationTest {

    private NotificationReceiverValidator validator;

    @BeforeEach
    void initializeValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = new NotificationReceiverValidator( factory.getValidator() );
    }

    @Test
    void invalidEmptyNotification() {

        // GIVEN
        Notification n = Notification.builder()
                .build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationNumberByField( errors, "documents", 1 );
        assertConstraintViolationNumberByField( errors, "sender",1 );
        assertConstraintViolationNumberByField( errors, "recipients", 1 );
        assertConstraintViolationNumberByField( errors, "paNotificationId", 1 );
        assertConstraintViolationNumberByField( errors, "subject",1 );
        Assertions.assertEquals( 5, errors.size() );
    }

    @Test
    void checkThrowMechanism() {
        // GIVEN
        Notification n = Notification.builder()
                .build();

        // WHEN
        Executable todo = () -> { validator.checkNewNotificationBeforeInsertAndThrow(n); };

        // THEN
        PnValidationException validationException;
        validationException = Assertions.assertThrows(PnValidationException.class, todo );

        Set<ConstraintViolation<Notification>> errors = validationException.getValidationErrors();
        assertConstraintViolationNumberByField( errors, "documents", 1 );
        assertConstraintViolationNumberByField( errors, "sender",1 );
        assertConstraintViolationNumberByField( errors, "recipients", 1 );
        assertConstraintViolationNumberByField( errors, "paNotificationId", 1 );
        assertConstraintViolationNumberByField( errors, "subject",1 );
        Assertions.assertEquals( 5, errors.size() );
    }


        @Test
    void invalidEmptyCollections() {

        // GIVEN
        Notification n = Notification.builder()
                .paNotificationId( "protocol1" )
                .subject( "subject" )
                .sender(NotificationSender.builder().build())
                .recipients( Collections.emptyList() )
                .documents( Collections.emptyList() )
                .build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationNumberByField( errors, "sender.paId",1 );
        assertConstraintViolationNumberByField( errors, "documents", 1 );
        assertConstraintViolationNumberByField( errors, "recipients", 1 );
        Assertions.assertEquals( 3, errors.size() );
    }

    @Test
    void invalidNullValuesInCollections() {

        // GIVEN
        Notification n = Notification.builder()
                .paNotificationId( "protocol1" )
                .subject( "subject" )
                .sender(NotificationSender.builder()
                        .paId("paId")
                        .build()
                )
                .recipients( Collections.singletonList( null ) )
                .documents( Collections.singletonList( null ) )
                .build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationNumberByField( errors, "documents[0]", 1 );
        assertConstraintViolationNumberByField( errors, "recipients[0]", 1 );
        Assertions.assertEquals( 2, errors.size() );
    }

    @Test
    void invalidDocumentAndRecipientWithEmptyFields() {

        // GIVEN
        Notification n = Notification.builder()
                .paNotificationId( "protocol1" )
                .subject( "subject" )
                .sender(NotificationSender.builder()
                        .paId("paId")
                        .build()
                )
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .build())
                )
                .documents( Collections.singletonList(NotificationAttachment.builder()
                        .build())
                )
                .build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationNumberByField( errors, "documents[0].contentType", 1 );
        assertConstraintViolationNumberByField( errors, "documents[0].body", 1 );
        assertConstraintViolationNumberByField( errors, "documents[0].digests", 1 );
        assertConstraintViolationNumberByField( errors, "recipients[0].taxId", 1 );
        assertConstraintViolationNumberByField( errors, "recipients[0].denomination", 1 );
        assertConstraintViolationNumberByField( errors, "recipients[0].digitalDomicile", 1 );
        Assertions.assertEquals( 6, errors.size() );
    }

    @Test
    void invalidDocumentAndRecipientWithEmptyDigestsAndDigitalDomicile() {

        // GIVEN
        Notification n = Notification.builder()
                .paNotificationId( "protocol1" )
                .subject( "subject" )
                .sender(NotificationSender.builder()
                        .paId("paId")
                        .build()
                )
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .taxId("FiscalCode")
                        .denomination("Nome Cognome / Ragione Sociale")
                        .digitalDomicile( DigitalAddress.builder().build() )
                        .build())
                )
                .documents( Collections.singletonList(NotificationAttachment.builder()
                        .body("Body")
                        .contentType("Content/Type")
                        .digests( NotificationAttachment.Digests.builder().build())
                        .build())
                )
                .build();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        assertConstraintViolationNumberByField( errors, "documents[0].digests.sha256", 1 );
        assertConstraintViolationNumberByField( errors, "recipients[0].digitalDomicile.address", 1 );
        assertConstraintViolationNumberByField( errors, "recipients[0].digitalDomicile.type", 1 );
        Assertions.assertEquals( 3, errors.size() );
    }

    @Test
    void validDocumentAndRecipientWithoutPayments() {

        // GIVEN
        Notification n = validDocumentWithoutPayments();

        // WHEN
        Set<ConstraintViolation<Notification>> errors;
        errors = validator.checkNewNotificationBeforeInsert( n );

        // THEN
        Assertions.assertEquals( 0, errors.size() );
    }



    @Test
    void invalidPecAddress() {

        // GIVEN
        Notification n = validDocumentWithoutPayments( );
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
        assertConstraintViolationNumberByField( errors, "recipients[0].digitalDomicile.address", 1 );
        Assertions.assertEquals( 1, errors.size() );
    }

    private <T> void assertConstraintViolationNumberByField( Set<ConstraintViolation<T>> set, String propertyPath, long number) {
        long actual = set.stream()
                .filter( cv -> propertyPathToString( cv.getPropertyPath() ).equals( propertyPath ) )
                .count();
        Assertions.assertEquals( number, actual, "expected validation errors on " + propertyPath );
    }

    private static String propertyPathToString( Path propertyPath ) {
        return propertyPath.toString().replaceFirst(".<[^>]*>$", "");
    }

    private Notification validDocumentWithoutPayments() {
        Notification n = Notification.builder()
                .paNotificationId( "protocol1" )
                .subject( "subject" )
                .sender(NotificationSender.builder()
                        .paId("paId")
                        .build()
                )
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .taxId("FiscalCode")
                        .denomination("Nome Cognome / Ragione Sociale")
                        .digitalDomicile( DigitalAddress.builder()
                                .type( DigitalAddressType.PEC )
                                .address("account@domain.it")
                                .build()
                        )
                        .build())
                )
                .documents( Collections.singletonList(NotificationAttachment.builder()
                        .body("Body")
                        .contentType("Content/Type")
                        .digests( NotificationAttachment.Digests.builder()
                                .sha256("sha256")
                                .build()
                        )
                        .build())
                )
                .build();
        return n;
    }
}
