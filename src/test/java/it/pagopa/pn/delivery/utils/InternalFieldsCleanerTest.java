package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        return new InternalNotification(FullSentNotificationV11.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId("pa_02")
                .notificationStatus(NotificationStatus.ACCEPTED)
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .internalId( "internalId" )
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(NotificationDigitalAddress.builder()
                                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationDocument.builder()
                                .ref(NotificationAttachmentBodyRef.builder()
                                        .key("doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationDocument.builder()
                                .ref(NotificationAttachmentBodyRef.builder()
                                        .key("doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .recipientIds(Collections.singletonList("recipientId"))
                .sourceChannel( "B2B" )
                .timeline(Collections.singletonList(TimelineElementV11.builder().build()))
                .notificationStatusHistory(Collections.singletonList(NotificationStatusHistoryElement.builder()
                        .status(NotificationStatus.ACCEPTED)
                        .build()))
                .build());
    }

}
