package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV21;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.in;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InternalFieldsCleanerTest {

    @Test
    void cleanInternalIdTest() {
        var recipients = List.of(
                new NotificationRecipient().internalId("internal1").denomination("den1"),
                new NotificationRecipient().internalId("internal2").denomination("den2")
        );

        InternalFieldsCleaner.cleanInternalId(recipients);

        assertThat(recipients.get(0).getInternalId()).isNull();
        assertThat(recipients.get(0).getDenomination()).isEqualTo("den1");
        assertThat(recipients.get(1).getInternalId()).isNull();
        assertThat(recipients.get(1).getDenomination()).isEqualTo("den2");
    }

    @Test
    void cleanInternalIdWithEmptyTest() {

        assertDoesNotThrow(() -> InternalFieldsCleaner.cleanInternalId(List.of()));
    }

    @Test
    void cleanInternalIdWithNullTest() {
        NotificationRecipient notificationRecipient = null;
        assertDoesNotThrow(() -> InternalFieldsCleaner.cleanInternalId(notificationRecipient));
    }

    @Test
    void cleanInternalNotificationFields() {
        InternalNotification internalNotification = newNotification();
        Assertions.assertNotNull( internalNotification.getSourceChannel() );
        Assertions.assertNotNull( internalNotification.getRecipientIds() );
        Assertions.assertNotNull( internalNotification.getRecipients().get( 0 ).getInternalId() );
        InternalFieldsCleaner.cleanInternalFields( internalNotification );
        Assertions.assertNull( internalNotification.getSourceChannel() );
        Assertions.assertNull( internalNotification.getRecipientIds() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getInternalId() );
    }

    @Test
    void cleanInternalNotificationFieldsWhitNull() {
        InternalNotification internalNotification = null;
        assertDoesNotThrow(() -> InternalFieldsCleaner.cleanInternalFields( internalNotification ) );

    }

    private InternalNotification newNotification() {
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setSourceChannel("sourceChannel");
        internalNotification.setRecipientIds(List.of("12"));
        internalNotification.setRecipients(
                List.of(
                        NotificationRecipient.builder()
                                .internalId("internalId")
                                .recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
                                .taxId("taxId")
                                .physicalAddress(it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress.builder().build())
                                .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder().build())
                                .payments(List.of(NotificationPaymentInfo.builder().build()))
                                .build()));
        internalNotification.setIun("IUN_01");
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
