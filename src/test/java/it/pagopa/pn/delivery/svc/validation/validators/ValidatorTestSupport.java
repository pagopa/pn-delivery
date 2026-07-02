package it.pagopa.pn.delivery.svc.validation.validators;

import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.F24Payment;
import it.pagopa.pn.delivery.models.internal.notification.MetadataAttachment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.InformalNotificationContext;
import it.pagopa.pn.delivery.svc.validation.context.LegalNotificationContext;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV24;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class ValidatorTestSupport {

    public static final String DEFAULT_CX_ID = "PA123";
    public static final String DEFAULT_SENDER_TAX_ID = "12345678901";

    private ValidatorTestSupport() {
    }

    public static LegalNotificationContext legalContext(InternalNotification payload) {
        LegalNotificationContext context = new LegalNotificationContext();
        context.setPayload(payload);
        context.setCxId(DEFAULT_CX_ID);
        return context;
    }

    public static InformalNotificationContext informalContext(InternalNotification payload) {
        return InformalNotificationContext.builder()
                .payload(payload)
                .cxId(DEFAULT_CX_ID)
                .build();
    }

    public static InternalNotification notification(List<NotificationRecipient> recipients, List<NotificationDocument> documents) {
        InternalNotification notification = new InternalNotification();
        notification.setSenderTaxId(DEFAULT_SENDER_TAX_ID);
        notification.setRecipients(new ArrayList<>(recipients));
        notification.setDocuments(documents == null ? new ArrayList<>() : new ArrayList<>(documents));
        notification.setAdditionalLanguages(List.of());
        return notification;
    }

    public static NotificationRecipient pfRecipient(String taxId, String denomination, NotificationPhysicalAddress physicalAddress, List<NotificationPaymentInfo> payments) {
        return recipient(NotificationRecipientV24.RecipientTypeEnum.PF, taxId, denomination, physicalAddress, payments);
    }

    public static NotificationRecipient pgRecipient(String taxId, String denomination, NotificationPhysicalAddress physicalAddress, List<NotificationPaymentInfo> payments) {
        return recipient(NotificationRecipientV24.RecipientTypeEnum.PG, taxId, denomination, physicalAddress, payments);
    }

    public static NotificationRecipient recipient(NotificationRecipientV24.RecipientTypeEnum recipientType, String taxId, String denomination, NotificationPhysicalAddress physicalAddress, List<NotificationPaymentInfo> payments) {
        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setRecipientType(recipientType);
        recipient.setTaxId(taxId);
        recipient.setDenomination(denomination);
        recipient.setPhysicalAddress(physicalAddress);
        recipient.setPayment(payments == null ? new ArrayList<>() : new ArrayList<>(payments));
        return recipient;
    }

    public static NotificationPhysicalAddress physicalAddress() {
        return physicalAddress("at", "address", "details", "80100", "Napoli", "municipalityDetails", "NA", "ITALIA");
    }

    public static NotificationPhysicalAddress physicalAddress(String at, String address, String addressDetails, String zip, String municipality, String municipalityDetails, String province, String foreignState) {
        return NotificationPhysicalAddress.builder()
                .at(at)
                .address(address)
                .addressDetails(addressDetails)
                .zip(zip)
                .municipality(municipality)
                .municipalityDetails(municipalityDetails)
                .province(province)
                .foreignState(foreignState)
                .build();
    }

    public static NotificationDocument document(String key, String sha256) {
        return NotificationDocument.builder()
                .contentType("application/pdf")
                .ref(NotificationAttachmentBodyRef.builder().key(key).versionToken("token").build())
                .digests(NotificationAttachmentDigests.builder().sha256(sha256).build())
                .title("title")
                .docIdx("DOC0")
                .build();
    }

    public static MetadataAttachment attachment(String key, String sha256, String contentType) {
        return MetadataAttachment.builder()
                .contentType(contentType)
                .ref(NotificationAttachmentBodyRef.builder().key(key).versionToken("token").build())
                .digests(NotificationAttachmentDigests.builder().sha256(sha256).build())
                .build();
    }

    public static NotificationPaymentInfo pagoPaPayment(String key, String sha256, String contentType) {
        return NotificationPaymentInfo.builder()
                .pagoPa(PagoPaPayment.builder()
                        .creditorTaxId("12345678901")
                        .noticeCode("123456789012345678")
                        .attachment(attachment(key, sha256, contentType))
                        .build())
                .build();
    }

    public static NotificationPaymentInfo f24Payment(String key, String sha256, String contentType) {
        return NotificationPaymentInfo.builder()
                .f24(F24Payment.builder()
                        .title("f24")
                        .metadataAttachment(attachment(key, sha256, contentType))
                        .build())
                .build();
    }

    public static void assertSuccess(ValidationResult result) {
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    public static void assertSingleErrorContaining(ValidationResult result, String detailFragment) {
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getDetail()).contains(detailFragment);
    }

    public static void assertSingleError(ValidationResult result, String code, String detailFragment) {
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(code);
        assertThat(result.getErrors().get(0).getDetail()).contains(detailFragment);
    }
}
