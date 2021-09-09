package it.pagopa.pn.delivery.svc.recivenotification;

import it.pagopa.pn.api.dto.notification.*;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.aop.TargetClassAware;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        assertConstraintViolationPresentByField( errors, "documents" );
        assertConstraintViolationPresentByField( errors, "sender" );
        assertConstraintViolationPresentByField( errors, "recipients" );
        assertConstraintViolationPresentByField( errors, "paNotificationId" );
        assertConstraintViolationPresentByField( errors, "subject" );
        Assertions.assertEquals( 5, errors.size() );
    }

    @Test
    void checkThrowMechanism() {
        // GIVEN
        Notification n = Notification.builder()
                .build();

        // WHEN
        Executable todo = () -> validator.checkNewNotificationBeforeInsertAndThrow(n);

        // THEN
        PnValidationException validationException;
        validationException = Assertions.assertThrows(PnValidationException.class, todo );

        Set<ConstraintViolation<Notification>> errors = validationException.getValidationErrors();
        assertConstraintViolationPresentByField( errors, "documents" );
        assertConstraintViolationPresentByField( errors, "sender" );
        assertConstraintViolationPresentByField( errors, "recipients" );
        assertConstraintViolationPresentByField( errors, "paNotificationId" );
        assertConstraintViolationPresentByField( errors, "subject" );
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
        assertConstraintViolationPresentByField( errors, "sender.paId" );
        assertConstraintViolationPresentByField( errors, "documents" );
        assertConstraintViolationPresentByField( errors, "recipients" );
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
        assertConstraintViolationPresentByField( errors, "documents[0]" );
        assertConstraintViolationPresentByField( errors, "recipients[0]" );
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
        assertConstraintViolationPresentByField( errors, "documents[0].contentType" );
        assertConstraintViolationPresentByField( errors, "documents[0].body" );
        assertConstraintViolationPresentByField( errors, "documents[0].digests" );
        assertConstraintViolationPresentByField( errors, "recipients[0].taxId" );
        assertConstraintViolationPresentByField( errors, "recipients[0].denomination" );
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile" );
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
        assertConstraintViolationPresentByField( errors, "documents[0].digests.sha256" );
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.address" );
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.type" );
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
        assertConstraintViolationPresentByField( errors, "recipients[0].digitalDomicile.address" );
        Assertions.assertEquals( 1, errors.size() );
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

    private Notification validDocumentWithoutPayments() {
        return Notification.builder()
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
    }
    private Notification validDocumentWithPayments() {
        return Notification.builder()
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
                                .sha256("a2ebebe0b177628318ecfc261870fbcd84d39e0fd46620fa36a90ddaaa556e39")
                                .build()
                        )
                        .build())
                )
                .payment( NotificationPaymentInfo.builder()
                        .f24(NotificationPaymentInfo.F24.builder()
                                .analog(NotificationAttachment.builder().body("Body")
                                        .contentType("Content/Type")
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("a2ebebe0b177628318ecfc261870fbcd84d39e0fd46620fa36a90ddaaa556e39")
                                                .build()
                                        ).build())
                                .flatRate(NotificationAttachment.builder().body("Body")
                                        .contentType("Content/Type")
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("a2ebebe0b177628318ecfc261870fbcd84d39e0fd46620fa36a90ddaaa556e39")
                                                .build()
                                        ).build())
                                .digital(NotificationAttachment.builder().body("Body")
                                        .contentType("Content/Type")
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("a2ebebe0b177628318ecfc261870fbcd84d39e0fd46620fa36a90ddaaa556e39")
                                                .build()
                                        ).build())
                                .build())
                        .build())
                .build();
    }

    private Notification validDocumentWithFalseDocumentsAndPayments() {
        return Notification.builder()
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
                        .digests( NotificationAttachment.Digests.builder()
                                .sha256("a2ebebe0b177628318ecfc261870fbcd84d39e0fd46620fa36a90ddaaa556e39")
                                .build()
                        )
                        .build())
                )
                .payment( NotificationPaymentInfo.builder()
                        .f24(NotificationPaymentInfo.F24.builder()
                                .analog(NotificationAttachment.builder().body("Body")
                                        .contentType("Content/Type")
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("a2ebebe0b177628318ecfc261870fbcd84d39e0fd46620fa36a90ddaaa556e39")
                                                .build()
                                        ).build())
                                .flatRate(NotificationAttachment.builder().body("Body")
                                        .contentType("Content/Type")
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("a2ebebe0b177628318ecfc261870fbcd84d39e0fd46620fa36a90ddaaa556e39")
                                                .build()
                                        ).build())
                                .digital(NotificationAttachment.builder().body("Body")
                                        .contentType("Content/Type")
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("a2ebebe0b177628318ecfc261870fbcd84d39e0fd46620fa36a90ddaaa556e39")
                                                .build()
                                        ).build())
                                .build())
                        .build())
                .build();
    }


    @Test
    public void testCheckNotificationAttachmentsBodyIsBase64() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Notification notification = validDocumentWithPayments();
        Method method = validator.getClass().getDeclaredMethod("checkNotificationAttachmentsBodyIsBase64", Notification.class);
        method.setAccessible(true);
        Assertions.assertTrue((Boolean) method.invoke(validator,notification));

    }

    @Test
    public void testCheckNotificationAttachmentsDigestIsSha256() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Notification notification = validDocumentWithPayments();
        Method method = validator.getClass().getDeclaredMethod("checkNotificationAttachmentsDigestIsSha256", Notification.class);
        method.setAccessible(true);
        Assertions.assertTrue((Boolean) method.invoke(validator,notification));

    }

    @Test
    public void testCheckNotificationAttachmentsDigestIsNotSha256() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Notification notification = validDocumentWithFalseDocumentsAndPayments();
        Method method = validator.getClass().getDeclaredMethod("checkNotificationAttachmentsDigestIsSha256", Notification.class);
        method.setAccessible(true);
        Assertions.assertFalse((Boolean) method.invoke(validator,notification));

    }



    @Test
    public void testCheckF24AttachmentsAreBase64() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Notification notification = validDocumentWithPayments();
        Method method = validator.getClass().getDeclaredMethod("checkF24AttachmentsAreBase64", Notification.class);
        method.setAccessible(true);
        Assertions.assertTrue((Boolean) method.invoke(validator,notification));

    }

    @Test
    public void testCheckF24AttachmentsArenotBase64() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Notification notification = validDocumentWithoutPayments();
        Method method = validator.getClass().getDeclaredMethod("checkF24AttachmentsAreBase64", Notification.class);
        method.setAccessible(true);
        Assertions.assertFalse((Boolean) method.invoke(validator,notification));

    }

}
