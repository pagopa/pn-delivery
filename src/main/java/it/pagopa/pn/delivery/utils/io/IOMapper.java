package it.pagopa.pn.delivery.utils.io;

import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategoryV20;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.F24Payment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class IOMapper {

    private static final String URL_ATTACHMENT = "/delivery/notifications/received/{iun}/attachments/documents/{indexDocument}";
    private static final String URL_ATTACHMENT_F24 = "/delivery/notifications/received/{iun}/attachments/payment/F24/?attachmentIdx={indexDocument}";
    private static final String F24_DOCUMENT_TYPE = "application/pdf";

    private final ModelMapper modelMapper;

    public ThirdPartyMessage mapToThirdPartMessage(InternalNotification internalNotification, boolean isCancelled) {
        if(internalNotification == null) return null;

        IOReceivedNotification details = mapToDetails(internalNotification, isCancelled);
        List<ThirdPartyAttachment> attachments = mapToThirdPartyAttachment(internalNotification);

        return ThirdPartyMessage.builder()
                .attachments(attachments)
                .details(details)
                .build();
    }

    public IOReceivedNotification mapToDetails(InternalNotification internalNotification, boolean isCancelled) {
        if(internalNotification == null) {
            return null;
        }

        IOReceivedNotification ioReceivedNotification = IOReceivedNotification.builder()
                .subject(internalNotification.getSubject())
                .iun(internalNotification.getIun())
                .notificationStatusHistory(convertNotificationStatusHistory(internalNotification.getNotificationStatusHistory()))
                ._abstract(internalNotification.get_abstract())
                .senderDenomination(internalNotification.getSenderDenomination())
                .build();

        it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient filteredNotificationRecipient = filterRecipient(internalNotification);
        if(filteredNotificationRecipient != null) {
            ioReceivedNotification.setRecipients(duplicateRecipientForEachPagoPaPayment(filteredNotificationRecipient));
        }

        if (isCancelled) {
            ioReceivedNotification.setIsCancelled(true);

            // solo se la notifica è annullata, ritorno eventuali record di pagamento completati
            // NB: la timeline è GIA' FILTRATA per recipientIndex
            ioReceivedNotification.setCompletedPayments(internalNotification.getTimeline()
                    .stream()
                    .filter(x -> x.getCategory().equals(TimelineElementCategoryV20.PAYMENT))
                    .map(x -> x.getDetails().getNoticeCode())
                    .toList());
        }

        return ioReceivedNotification;
    }

    private List<NotificationStatusHistoryElement> convertNotificationStatusHistory(List<it.pagopa.pn.delivery.models.internal.notification.NotificationStatusHistoryElement> notificationStatusHistory) {
        return notificationStatusHistory.stream()
                .map(notificationStatusHistoryElement -> NotificationStatusHistoryElement.builder()
                        .status(notificationStatusHistoryElement.getStatus().getValue())
                        .activeFrom(notificationStatusHistoryElement.getActiveFrom())
                        .relatedTimelineElements(notificationStatusHistoryElement.getRelatedTimelineElements())
                        .build()
                )
                .toList();
    }

    private List<NotificationRecipient> duplicateRecipientForEachPagoPaPayment(it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient filteredNotificationRecipient) {
        // Estraggo tutti i pagamenti pagoPa del destinatario
        List<PagoPaPayment> pagoPaPayments = filteredNotificationRecipient.getPayments()
                .stream()
                .map(it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo::getPagoPa)
                .filter(Objects::nonNull)
                .toList();

        // Se non ci sono pagamenti pagoPa restituisco un destinatario.
        if(pagoPaPayments.isEmpty()) {
            return List.of(NotificationRecipient.builder()
                    .recipientType(filteredNotificationRecipient.getRecipientType().getValue())
                    .taxId(filteredNotificationRecipient.getTaxId())
                    .denomination(filteredNotificationRecipient.getDenomination())
                    .build());
        }

        // Se ci sono pagamenti pagoPa restituisco un destinatario per ogni pagamento.
        return pagoPaPayments.stream()
                .map(payment -> NotificationRecipient.builder()
                        .recipientType(filteredNotificationRecipient.getRecipientType().getValue())
                        .taxId(filteredNotificationRecipient.getTaxId())
                        .denomination(filteredNotificationRecipient.getDenomination())
                        .payment(NotificationPaymentInfo.builder()
                                .creditorTaxId(payment.getCreditorTaxId())
                                .noticeCode(payment.getNoticeCode())
                                .build()
                        )
                        .build()
                )
                .toList();
    }

    public List<ThirdPartyAttachment> mapToThirdPartyAttachment(InternalNotification internalNotification) {
        List<ThirdPartyAttachment> thirdPartyAttachments = new ArrayList<>();
        List<NotificationDocument> documents = internalNotification.getDocuments();

        it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient filteredNotificationRecipient = filterRecipient(internalNotification);

        if(recipientHasF24Payments(filteredNotificationRecipient)) {
            thirdPartyAttachments.addAll(mapF24PaymentsToThirdPartyAttachment(filteredNotificationRecipient, internalNotification.getIun()));
        }

        if(!documents.isEmpty()) {
            thirdPartyAttachments.addAll(mapDocumentsToThirdPartyAttachment(documents, internalNotification.getIun()));
        }

        return thirdPartyAttachments;
    }

    private it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient filterRecipient(InternalNotification internalNotification) {
        //prendo il primo perchè si suppone che ci sia un solo recipient con il taxId
        return internalNotification.getRecipients()
                .stream()
                .filter(rec -> StringUtils.hasText(rec.getTaxId()))
                .findFirst()
                .orElse(null);
    }

    private boolean recipientHasF24Payments(it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient filteredNotificationRecipient) {
        if(filteredNotificationRecipient == null || filteredNotificationRecipient.getPayments() == null) {
            return false;
        }

        return filteredNotificationRecipient.getPayments()
                .stream()
                .anyMatch(notificationPaymentInfo -> notificationPaymentInfo.getF24() != null);

    }

    private List<ThirdPartyAttachment> mapF24PaymentsToThirdPartyAttachment(it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient filteredNotificationRecipient, String iun) {
        List<ThirdPartyAttachment> thirdPartyAttachments = new ArrayList<>();

        for(int i = 0; i < filteredNotificationRecipient.getPayments().size(); i++) {
            var f24Payment = filteredNotificationRecipient.getPayments().get(i).getF24();
            if(f24Payment != null) {
                thirdPartyAttachments.add(mapF24ToThirdPartyAttachment(f24Payment, i, iun));
            }
        }

        return thirdPartyAttachments;
    }

    public ThirdPartyAttachment mapF24ToThirdPartyAttachment(F24Payment f24Payment, int indexDocument, String iun) {
        if(f24Payment == null) return null;

        return ThirdPartyAttachment.builder()
                .contentType(F24_DOCUMENT_TYPE)
                .id(iun + "_F24_" + indexDocument)
                .name(f24Payment.getTitle())
                .category(ThirdPartyAttachment.CategoryEnum.F24)
                .url(URL_ATTACHMENT_F24.replace("{iun}", iun).replace("{indexDocument}", indexDocument + ""))
                .build();
    }

    private List<ThirdPartyAttachment> mapDocumentsToThirdPartyAttachment(List<NotificationDocument> documents, String iun) {
        return IntStream
                .range(0, documents.size())
                .mapToObj(index -> mapToThirdPartyAttachment(documents.get(index), index, iun))
                .toList();
    }

    public ThirdPartyAttachment mapToThirdPartyAttachment(NotificationDocument document, int indexDocument, String iun) {
        if(document == null) return null;

        return ThirdPartyAttachment.builder()
                .contentType(document.getContentType())
                .id(iun + "_DOC" + indexDocument)
                .name(document.getTitle())
                .category(ThirdPartyAttachment.CategoryEnum.DOCUMENT)
                .url(URL_ATTACHMENT.replace("{iun}", iun).replace("{indexDocument}", indexDocument + ""))
                .build();
    }
}