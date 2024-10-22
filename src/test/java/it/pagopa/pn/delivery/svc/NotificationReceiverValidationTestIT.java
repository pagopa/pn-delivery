package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.LocalStackTestConfig;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SpringBootTest
@Import(LocalStackTestConfig.class)
@TestPropertySource(properties = {
        "pn.delivery.max-recipients-count=0",
        "pn.delivery.max-attachments-count=0"
})
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

        NewNotificationRequestV24 notificationRequest = newNotification( INVALID_TAX_ID_NOT_IN_WHITE_LIST );

        Set<ConstraintViolation<NewNotificationRequestV24>> constraintViolations = validator.checkNewNotificationRequestBeforeInsert(notificationRequest);

        assertConstraintViolationPresentByMessage(constraintViolations, "Invalid taxId for recipient 0", 1);
    }

    @Test
    void taxIdInWhiteListInvalidCF() {

        NewNotificationRequestV24 notificationRequest = newNotification( INVALID_TAX_ID_IN_WHITE_LIST );

        Set<ConstraintViolation<NewNotificationRequestV24>> constraintViolations = validator.checkNewNotificationRequestBeforeInsert(notificationRequest);

        assertConstraintViolationPresentByMessage(constraintViolations, "Invalid taxId for recipient 0", 0);

    }

    @Test
    void taxIdNotInWhiteListMultipleInvalidCF() {

        NewNotificationRequestV24 notificationRequest = newNotification( INVALID_TAX_ID_IN_WHITE_LIST );
        notificationRequest.addRecipientsItem( NotificationRecipientV23.builder()
                        .taxId( INVALID_TAX_ID_NOT_IN_WHITE_LIST )
                        .recipientType(NotificationRecipientV23.RecipientTypeEnum.PF)
                .build() );

        Set<ConstraintViolation<NewNotificationRequestV24>> constraintViolations = validator.checkNewNotificationRequestBeforeInsert(notificationRequest);

        assertConstraintViolationPresentByMessage(constraintViolations, "Invalid taxId for recipient 1", 1);
    }

    private NewNotificationRequestV24 newNotification( String recipientTaxId ) {
        List<NotificationRecipientV23> recipients = new ArrayList<>();
        recipients.add(
                NotificationRecipientV23.builder().recipientType(NotificationRecipientV23.RecipientTypeEnum.PF)
                        .taxId( recipientTaxId ).denomination("Nome Cognome / Ragione Sociale")
                        .digitalDomicile(NotificationDigitalAddress.builder()
                                .type(NotificationDigitalAddress.TypeEnum.PEC).address("account@domain.it").build())
                        .physicalAddress(NotificationPhysicalAddress.builder().address("Indirizzo").zip("zip")
                                .province("province").municipality("municipality").at("at").build())
                        .payments(new ArrayList<>())
                        .build());
        return NewNotificationRequestV24.builder().senderDenomination("Sender Denomination")
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .idempotenceToken("IUN_01").paProtocolNumber("protocol1").subject("subject")
                .senderTaxId("paId").recipients(recipients).build();
    }

    private <T> void assertConstraintViolationPresentByMessage(Set<ConstraintViolation<T>> set,
                                                               String message, long expected) {
        long actual = set.stream().filter(cv -> cv.getMessage().equals(message)).count();
        Assertions.assertEquals(expected, actual);
    }

}
