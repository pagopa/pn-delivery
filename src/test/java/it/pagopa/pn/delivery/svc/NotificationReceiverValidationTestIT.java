package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.LocalStackTestConfig;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SpringBootTest
@Import(LocalStackTestConfig.class)
class NotificationReceiverValidationTestIT {

    private static final String IUN = "FAKE-FAKE-FAKE-202209-F-1";
    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
    private static final String INVALID_TAX_ID_NOT_IN_WHITE_LIST = "ASDASDASD";
    private static final String INVALID_TAX_ID_IN_WHITE_LIST = "EEEEEE00E00E000A";

    @Autowired
    private ValidateUtils validateUtils;

    @Autowired
    private NotificationReceiverValidator validator;

    @Test
    void taxIdNotInWhiteListInvalidCF() {

        NewNotificationRequest notificationRequest = newNotification( INVALID_TAX_ID_NOT_IN_WHITE_LIST );

        Set<ConstraintViolation<NewNotificationRequest>> constraintViolations = validator.checkNewNotificationRequestBeforeInsert(notificationRequest);

        assertConstraintViolationPresentByMessage(constraintViolations, "Invalid taxId for recipient 0", 1);
    }

    @Test
    void taxIdInWhiteListInvalidCF() {

        NewNotificationRequest notificationRequest = newNotification( INVALID_TAX_ID_IN_WHITE_LIST );

        Set<ConstraintViolation<NewNotificationRequest>> constraintViolations = validator.checkNewNotificationRequestBeforeInsert(notificationRequest);

        assertConstraintViolationPresentByMessage(constraintViolations, "Invalid taxId for recipient 0", 0);

    }

    @Test
    void taxIdNotInWhiteListMultipleInvalidCF() {

        NewNotificationRequest notificationRequest = newNotification( INVALID_TAX_ID_IN_WHITE_LIST );
        notificationRequest.addRecipientsItem( NotificationRecipient.builder()
                        .taxId( INVALID_TAX_ID_NOT_IN_WHITE_LIST )
                .build() );

        Set<ConstraintViolation<NewNotificationRequest>> constraintViolations = validator.checkNewNotificationRequestBeforeInsert(notificationRequest);

        assertConstraintViolationPresentByMessage(constraintViolations, "Invalid taxId for recipient 1", 1);
    }

    private NewNotificationRequest newNotification( String recipientTaxId ) {
        List<NotificationRecipient> recipients = new ArrayList<>();
        recipients.add(
                NotificationRecipient.builder().recipientType(NotificationRecipient.RecipientTypeEnum.PF)
                        .taxId( recipientTaxId ).denomination("Nome Cognome / Ragione Sociale")
                        .digitalDomicile(NotificationDigitalAddress.builder()
                                .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
                        .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo").zip("zip")
                                .province("province").municipality("municipality").at("at").build())
                        .payment(NotificationPaymentInfo.builder().noticeCode("noticeCode")
                                .noticeCodeAlternative("noticeCodeAlternative").build())
                        .build());
        return NewNotificationRequest.builder().senderDenomination("Sender Denomination")
                .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject")
                .senderTaxId("paId").recipients(recipients).build();
    }

    private <T> void assertConstraintViolationPresentByMessage(Set<ConstraintViolation<T>> set,
                                                               String message, long expected) {
        long actual = set.stream().filter(cv -> cv.getMessage().equals(message)).count();
        Assertions.assertEquals(expected, actual);
    }

}
