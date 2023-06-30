package it.pagopa.pn.delivery.utils.io;

import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.IOReceivedNotification;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ThirdPartyAttachment;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ThirdPartyMessage;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.NotificationStatusHistoryElement;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

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
                        .recipients(List.of(NotificationRecipient.builder()
                                        .denomination("DENOMINATION")
                                        .taxId("TAXID")
                                        .recipientType("PF")
                                .build()))
                        .notificationStatusHistory(List.of(NotificationStatusHistoryElement.builder().status("ACCEPTED").build()))
                        .build())
                .build();

        ThirdPartyMessage actualValue = ioMapper.mapToThirdPartMessage(internalNotification);

        assertThat(actualValue).isEqualTo(expectedValue);
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
        FullSentNotification fullSentNotification = FullSentNotification.builder()
                .subject("SUBJECT")
                .iun("IUN")
                ._abstract("ABSTRACT")
                .recipients(List.of(
                        it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient.builder()
                                .internalId("INTERNALID")
                                .taxId("TAXID")
                                .denomination("DENOMINATION")
                                .recipientType(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient.RecipientTypeEnum.PF)
                                .physicalAddress(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPhysicalAddress.builder().address("ADDRESS").build())
                                .build()
                ))
                .senderDenomination("SENDERDENOMINATION")
                .notificationStatusHistory(List.of(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.builder().status(NotificationStatus.ACCEPTED).build()))
                .documents(List.of(notificationDocument()))
                .build();

        return new InternalNotification(fullSentNotification);
    }
}
