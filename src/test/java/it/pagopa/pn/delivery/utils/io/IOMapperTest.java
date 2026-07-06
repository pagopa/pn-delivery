package it.pagopa.pn.delivery.utils.io;

import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.IOReceivedNotification;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.NotificationStatusHistoryElement;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ThirdPartyAttachment;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ThirdPartyMessage;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategoryV28;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementDetailsV28;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV28;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.LegalNotificationDetail;
import it.pagopa.pn.delivery.models.internal.notification.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IOMapperTest {

    private IOMapper ioMapper;

    @BeforeEach
    void setup() {
        ioMapper = new IOMapper(new ModelMapper());
    }

    @Test
    void mapToThirdPartMessageReturnsNullWhenNotificationDetailIsNull() {
        ThirdPartyMessage actualValue = ioMapper.mapToThirdPartMessage(null, false);

        assertThat(actualValue).isNull();
    }

    @Test
    void mapToDetailsReturnsNullWhenNotificationDetailIsNull() {
        IOReceivedNotification actualValue = ioMapper.mapToDetails(null, false);

        assertThat(actualValue).isNull();
    }

    @Test
    void mapToDetailsReturnsNullWhenInternalNotificationIsNull() {
        LegalNotificationDetail notification = new LegalNotificationDetail();

        IOReceivedNotification actualValue = ioMapper.mapToDetails(notification, false);

        assertThat(actualValue).isNull();
    }

    @Test
    void mapToThirdPartyAttachmentReturnsEmptyListWhenNotificationDetailIsNull() {
        List<ThirdPartyAttachment> actualValue = ioMapper.mapToThirdPartyAttachment(null);

        assertThat(actualValue).isNotNull();
        assertThat(actualValue).isEmpty();
    }

    @Test
    void mapToThirdPartyAttachmentReturnsEmptyListWhenInternalNotificationIsNull() {
        LegalNotificationDetail notification = new LegalNotificationDetail();

        List<ThirdPartyAttachment> actualValue = ioMapper.mapToThirdPartyAttachment(notification);

        assertThat(actualValue).isNotNull();
        assertThat(actualValue).isEmpty();
    }

    @Test
    void mapToDetailsMapsBaseFieldsAndRecipients() {
        LegalNotificationDetail notification = newLegalNotification();

        IOReceivedNotification actualValue = ioMapper.mapToDetails(notification, false);

        assertThat(actualValue).isNotNull();
        assertThat(actualValue.getIun()).isEqualTo("IUN");
        assertThat(actualValue.getSubject()).isEqualTo("SUBJECT");
        assertThat(actualValue.getAbstract()).isEqualTo("ABSTRACT");
        assertThat(actualValue.getSenderDenomination()).isEqualTo("SENDERDENOMINATION");

        assertThat(actualValue.getRecipients()).hasSize(1);
        assertThat(actualValue.getRecipients().get(0).getRecipientType()).isEqualTo("PF");
        assertThat(actualValue.getRecipients().get(0).getTaxId()).isEqualTo("Codice Fiscale 01");
        assertThat(actualValue.getRecipients().get(0).getDenomination()).isEqualTo("Nome Cognome/Ragione Sociale");

        assertThat(actualValue.getNotificationStatusHistory()).hasSize(2);
        assertThat(actualValue.getNotificationStatusHistory())
                .extracting(NotificationStatusHistoryElement::getStatus)
                .containsExactly("ACCEPTED", "VIEWED");

        assertThat(actualValue.getIsCancelled()).isNull();
        assertThat(actualValue.getCompletedPayments()).isNull();
    }

    @Test
    void mapToDetailsSetsCancelledAndCompletedPaymentsWhenCancelled() {
        LegalNotificationDetail notification = newLegalNotificationWithPaymentTimeline("302000100000019421");

        IOReceivedNotification actualValue = ioMapper.mapToDetails(notification, true);

        assertThat(actualValue).isNotNull();
        assertThat(actualValue.getIsCancelled()).isTrue();
        assertThat(actualValue.getCompletedPayments()).containsExactly("302000100000019421");
    }

    @Test
    void mapToDetailsSetsCancelledAndEmptyCompletedPaymentsWhenCancelledWithoutPaymentTimeline() {
        LegalNotificationDetail notification = newLegalNotification();

        IOReceivedNotification actualValue = ioMapper.mapToDetails(notification, true);

        assertThat(actualValue).isNotNull();
        assertThat(actualValue.getIsCancelled()).isTrue();
        assertThat(actualValue.getCompletedPayments()).isEmpty();
    }

    @Test
    void mapToDetailsDuplicatesRecipientForEachPagoPaPayment() {
        LegalNotificationDetail notification = newLegalNotification();
        notification.getNotification().setRecipients(List.of(
                NotificationRecipient.builder()
                        .recipientType(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV24.RecipientTypeEnum.PF)
                        .taxId("taxId-1")
                        .denomination("Recipient 1")
                        .payments(List.of(
                                NotificationPaymentInfo.builder()
                                        .pagoPa(PagoPaPayment.builder()
                                                .creditorTaxId("creditor-1")
                                                .noticeCode("notice-1")
                                                .build())
                                        .build(),
                                NotificationPaymentInfo.builder()
                                        .pagoPa(PagoPaPayment.builder()
                                                .creditorTaxId("creditor-2")
                                                .noticeCode("notice-2")
                                                .build())
                                        .build()
                        ))
                        .build()
        ));

        IOReceivedNotification actualValue = ioMapper.mapToDetails(notification, false);

        assertThat(actualValue).isNotNull();
        assertThat(actualValue.getRecipients()).hasSize(2);
        assertThat(actualValue.getRecipients().get(0).getPayment().getCreditorTaxId()).isEqualTo("creditor-1");
        assertThat(actualValue.getRecipients().get(0).getPayment().getNoticeCode()).isEqualTo("notice-1");
        assertThat(actualValue.getRecipients().get(1).getPayment().getCreditorTaxId()).isEqualTo("creditor-2");
        assertThat(actualValue.getRecipients().get(1).getPayment().getNoticeCode()).isEqualTo("notice-2");
    }

    @Test
    void mapToThirdPartMessageMapsDetailsAndAttachments() {
        LegalNotificationDetail notification = newLegalNotification();

        ThirdPartyMessage actualValue = ioMapper.mapToThirdPartMessage(notification, false);

        assertThat(actualValue).isNotNull();
        assertThat(actualValue.getDetails()).isNotNull();
        assertThat(actualValue.getDetails().getIun()).isEqualTo("IUN");
        assertThat(actualValue.getAttachments()).hasSize(1);

        ThirdPartyAttachment attachment = actualValue.getAttachments().get(0);
        assertThat(attachment.getId()).isEqualTo("IUN_DOC0");
        assertThat(attachment.getName()).isEqualTo("TITLE.pdf");
        assertThat(attachment.getContentType()).isEqualTo("application/pdf");
        assertThat(attachment.getCategory()).isEqualTo(ThirdPartyAttachment.CategoryEnum.DOCUMENT);
        assertThat(attachment.getUrl()).isEqualTo("/delivery/notifications/received/IUN/attachments/documents/0");
    }

    @Test
    void mapToThirdPartyAttachmentMapsDocumentAttachment() {
        LegalNotificationDetail notification = newLegalNotification();

        List<ThirdPartyAttachment> actualValue = ioMapper.mapToThirdPartyAttachment(notification);

        assertThat(actualValue).hasSize(1);

        ThirdPartyAttachment attachment = actualValue.get(0);
        assertThat(attachment.getId()).isEqualTo("IUN_DOC0");
        assertThat(attachment.getName()).isEqualTo("TITLE.pdf");
        assertThat(attachment.getContentType()).isEqualTo("application/pdf");
        assertThat(attachment.getCategory()).isEqualTo(ThirdPartyAttachment.CategoryEnum.DOCUMENT);
        assertThat(attachment.getUrl()).isEqualTo("/delivery/notifications/received/IUN/attachments/documents/0");
    }

    @Test
    void mapToThirdPartyAttachmentMapsDefaultNameWhenTitleIsNull() {
        ThirdPartyAttachment actualValue = ioMapper.mapToThirdPartyAttachment(notificationDocument(null), 0, "IUN");

        assertThat(actualValue).isNotNull();
        assertThat(actualValue.getId()).isEqualTo("IUN_DOC0");
        assertThat(actualValue.getName()).isEqualTo("IUN_DOC0.pdf");
        assertThat(actualValue.getCategory()).isEqualTo(ThirdPartyAttachment.CategoryEnum.DOCUMENT);
    }

    @Test
    void mapToThirdPartyAttachmentMapsDefaultNameWhenTitleIsBlank() {
        ThirdPartyAttachment actualValue = ioMapper.mapToThirdPartyAttachment(notificationDocument("   "), 0, "IUN");

        assertThat(actualValue).isNotNull();
        assertThat(actualValue.getId()).isEqualTo("IUN_DOC0");
        assertThat(actualValue.getName()).isEqualTo("IUN_DOC0.pdf");
        assertThat(actualValue.getCategory()).isEqualTo(ThirdPartyAttachment.CategoryEnum.DOCUMENT);
    }

    @Test
    void mapToThirdPartyAttachmentReturnsNullWhenDocumentIsNull() {
        ThirdPartyAttachment actualValue = ioMapper.mapToThirdPartyAttachment(null, 0, "IUN");

        assertThat(actualValue).isNull();
    }

    @Test
    void mapToThirdPartyAttachmentReturnsOnlyF24WhenDocumentsAreNull() {
        LegalNotificationDetail notification = newLegalNotification();
        notification.getNotification().setDocuments(null);
        notification.getNotification().setRecipients(List.of(
                NotificationRecipient.builder()
                        .recipientType(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV24.RecipientTypeEnum.PF)
                        .taxId("taxId")
                        .denomination("Recipient")
                        .payments(List.of(
                                NotificationPaymentInfo.builder()
                                        .f24(F24Payment.builder()
                                                .title("MODELLO_F24")
                                                .metadataAttachment(MetadataAttachment.builder()
                                                        .ref(NotificationAttachmentBodyRef.builder().key("f24-key").build())
                                                        .build())
                                                .build())
                                        .build()
                        ))
                        .build()
        ));

        List<ThirdPartyAttachment> actualValue = ioMapper.mapToThirdPartyAttachment(notification);

        assertThat(actualValue).hasSize(1);
        assertThat(actualValue.get(0).getId()).isEqualTo("IUN_F24_0");
        assertThat(actualValue.get(0).getName()).isEqualTo("MODELLO_F24.pdf");
        assertThat(actualValue.get(0).getCategory()).isEqualTo(ThirdPartyAttachment.CategoryEnum.F24);
    }

    @Test
    void mapToThirdPartyAttachmentMapsF24Attachment() {
        F24Payment f24Payment = F24Payment.builder()
                .title("F24_TITLE")
                .metadataAttachment(MetadataAttachment.builder()
                        .ref(NotificationAttachmentBodyRef.builder().key("f24-key").build())
                        .build())
                .build();

        ThirdPartyAttachment actualValue = ioMapper.mapF24ToThirdPartyAttachment(f24Payment, 2, "IUN");

        assertThat(actualValue).isNotNull();
        assertThat(actualValue.getId()).isEqualTo("IUN_F24_2");
        assertThat(actualValue.getName()).isEqualTo("F24_TITLE.pdf");
        assertThat(actualValue.getContentType()).isEqualTo("application/pdf");
        assertThat(actualValue.getCategory()).isEqualTo(ThirdPartyAttachment.CategoryEnum.F24);
        assertThat(actualValue.getUrl()).isEqualTo("/delivery/notifications/received/IUN/attachments/payment/F24/?attachmentIdx=2");
    }

    @Test
    void mapF24ToThirdPartyAttachmentReturnsNullWhenF24IsNull() {
        ThirdPartyAttachment actualValue = ioMapper.mapF24ToThirdPartyAttachment(null, 0, "IUN");

        assertThat(actualValue).isNull();
    }

    @Test
    void addFileExtensionIfMissingAddsPdfExtension() {
        String actualValue = IOMapper.addFileExtensionIfMissing("document", "application/pdf");

        assertThat(actualValue).isEqualTo("document.pdf");
    }

    @Test
    void addFileExtensionIfMissingDoesNotDuplicateExtension() {
        String actualValue = IOMapper.addFileExtensionIfMissing("document.pdf", "application/pdf");

        assertThat(actualValue).isEqualTo("document.pdf");
    }

    @Test
    void addFileExtensionIfMissingReturnsFileNameWhenContentTypeIsNull() {
        String actualValue = IOMapper.addFileExtensionIfMissing("document", null);

        assertThat(actualValue).isEqualTo("document");
    }

    @Test
    void addFileExtensionIfMissingHandlesNullFileName() {
        String actualValue = IOMapper.addFileExtensionIfMissing(null, "application/pdf");

        assertThat(actualValue).isEqualTo(".pdf");
    }

    @Test
    void getFileExtensionFromContentTypeReturnsPdf() {
        String actualValue = IOMapper.getFileExtensionFromContentType("application/pdf");

        assertThat(actualValue).isEqualTo("pdf");
    }

    @Test
    void getFileExtensionFromContentTypeReturnsEmptyStringForUnknownContentType() {
        String actualValue = IOMapper.getFileExtensionFromContentType("text/plain");

        assertThat(actualValue).isEqualTo("");
    }

    private LegalNotificationDetail newLegalNotification() {
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setIun("IUN");
        internalNotification.setSubject("SUBJECT");
        internalNotification.setAbstract("ABSTRACT");
        internalNotification.setSenderDenomination("SENDERDENOMINATION");
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setDocuments(List.of(
                it.pagopa.pn.delivery.models.internal.notification.NotificationDocument.builder()
                        .docIdx("DOC0")
                        .contentType("application/pdf")
                        .ref(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef.builder()
                                .key("doc-key")
                                .versionToken("versionToken")
                                .build())
                        .title("TITLE")
                        .build()
        ));
        internalNotification.setRecipients(List.of(
                NotificationRecipient.builder()
                        .recipientType(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV24.RecipientTypeEnum.PF)
                        .taxId("Codice Fiscale 01")
                        .denomination("Nome Cognome/Ragione Sociale")
                        .internalId("recipientInternalId")
                        .payments(List.of(NotificationPaymentInfo.builder().build()))
                        .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                                .type(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress.TypeEnum.PEC)
                                .address("account@dominio.it")
                                .build())
                        .build()
        ));

        LegalNotificationDetail legalNotificationDetail = new LegalNotificationDetail();
        legalNotificationDetail.setNotification(internalNotification);
        legalNotificationDetail.setNotificationStatus(NotificationStatusV26.ACCEPTED);
        legalNotificationDetail.setNotificationStatusHistory(List.of(
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26.builder()
                        .status(NotificationStatusV26.ACCEPTED)
                        .build(),
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26.builder()
                        .status(NotificationStatusV26.VIEWED)
                        .build()
        ));
        legalNotificationDetail.setTimeline(List.of(
                TimelineElementV28.builder()
                        .category(TimelineElementCategoryV28.AAR_CREATION_REQUEST)
                        .build()
        ));
        return legalNotificationDetail;
    }

    private LegalNotificationDetail newLegalNotificationWithPaymentTimeline(String noticeCode) {
        LegalNotificationDetail legalNotificationDetail = newLegalNotification();
        legalNotificationDetail.setTimeline(List.of(
                TimelineElementV28.builder()
                        .category(TimelineElementCategoryV28.PAYMENT)
                        .details(TimelineElementDetailsV28.builder().noticeCode(noticeCode).build())
                        .build()
        ));
        return legalNotificationDetail;
    }

    private it.pagopa.pn.delivery.models.internal.notification.NotificationDocument notificationDocument(String title) {
        return it.pagopa.pn.delivery.models.internal.notification.NotificationDocument.builder()
                .title(title)
                .contentType("application/pdf")
                .ref(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef.builder()
                        .key("key")
                        .build())
                .build();
    }
}