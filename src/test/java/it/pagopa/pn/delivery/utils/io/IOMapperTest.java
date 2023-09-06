package it.pagopa.pn.delivery.utils.io;

import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.IOReceivedNotification;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.NotificationStatusHistoryElement;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ThirdPartyAttachment;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ThirdPartyMessage;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class IOMapperTest {
    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";

    @Spy
    private ModelMapper modelMapper;

    @InjectMocks
    private IOMapper ioMapper;


    @Test
    void mapToThirdPartyMessage() {
        int indexDocument = 0;
        String iun = "IUN";
        InternalNotification internalNotification = internalNotification();

        ThirdPartyMessage expectedValue = ThirdPartyMessage.builder()
                .attachments(List.of(ThirdPartyAttachment.builder()
                        .contentType("application/pdf")
                        .id(iun + "_DOC" + indexDocument)
                        .name("TITLE")
                        .url("/delivery/notifications/received/IUN/attachments/documents/0")
                        .build()))
                .details(IOReceivedNotification.builder()
                        .iun("IUN")
                        .subject("SUBJECT")
                        ._abstract("ABSTRACT")
                        .senderDenomination("SENDERDENOMINATION")
                        .recipients(List.of(it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.NotificationRecipient.builder()
                                .denomination("DENOMINATION")
                                .taxId("TAXID")
                                .recipientType("PF")
                                .build()))
                        .notificationStatusHistory(List.of(NotificationStatusHistoryElement.builder().status("ACCEPTED").build()))
                        .build())
                .build();

        ThirdPartyMessage actualValue = ioMapper.mapToThirdPartMessage(internalNotification);

        assertThat(actualValue.getAttachments()).isEqualTo(expectedValue.getAttachments());
    }

    @Test
    void mapToThirdPartyMessageNotificationNull() {

        ThirdPartyMessage actualValue = ioMapper.mapToThirdPartMessage(null);

        assertThat(actualValue).isNull();
    }

    @Test
    void mapToDetailsNotificationNull() {

        IOReceivedNotification actualValue = ioMapper.mapToDetails(null);

        assertThat(actualValue).isNull();
    }

    @Test
    void mapToThirdPartyAttachmentWithDocumentNullTest() {

        ThirdPartyAttachment actualValue = ioMapper.mapToThirdPartyAttachment(null, 0, "IUN");

        assertThat(actualValue).isNull();

    }


    @Test
    void mapToThirdPartyAttachmentCollectionEmptyTest() {

        List<ThirdPartyAttachment> actualValue = ioMapper.mapToThirdPartyAttachment(List.of(), "IUN");

        assertThat(actualValue).isNotNull().isEmpty();

    }

    private NotificationDocument notificationDocument() {
        return NotificationDocument.builder()
                .title("TITLE")
                .contentType("application/pdf")
                .ref(NotificationAttachmentBodyRef.builder().key("key").build())
                .build();
    }

    private InternalNotification internalNotification() {
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setDocuments(List.of(it.pagopa.pn.delivery.models.internal.notification.NotificationDocument.builder()
                .docIdx("DOC0")
                .contentType("application/pdf")
                .title("TITLE")
                .build()));
        internalNotification.setNotificationStatusHistory(List.of(
                it.pagopa.pn.delivery.models.internal.notification.NotificationStatusHistoryElement.builder()
                        .status(NotificationStatus.ACCEPTED)
                        .build()
        ));
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
        internalNotification.setIun("IUN");
        internalNotification.setPaProtocolNumber("protocol_01");
        internalNotification.setSubject("SUBJECT");
        internalNotification.setCancelledIun("IUN_05");
        internalNotification.setCancelledIun("IUN_00");
        internalNotification.setSenderPaId("PA_ID");
        internalNotification.setAbstract("ABSTRACT");
        internalNotification.setSenderDenomination("SENDERDENOMINATION");
        internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
        internalNotification.setRecipients(Collections.singletonList(
                NotificationRecipient.builder()
                        .recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
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
