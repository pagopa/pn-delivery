package it.pagopa.pn.delivery.utils.io;

import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.IOReceivedNotification;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ThirdPartyAttachment;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ThirdPartyMessage;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDocument;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategoryV20;
import it.pagopa.pn.delivery.models.InternalNotification;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class IOMapper {

    private static final String URL_ATTACHMENT = "/delivery/notifications/received/{iun}/attachments/documents/{indexDocument}";

    private final ModelMapper modelMapper;

    public ThirdPartyMessage mapToThirdPartMessage(InternalNotification internalNotification) {
        if(internalNotification == null) return null;

        IOReceivedNotification details = mapToDetails(internalNotification);
        List<ThirdPartyAttachment> attachments = mapToThirdPartyAttachment(internalNotification.getDocuments(),
                internalNotification.getIun());

        return ThirdPartyMessage.builder()
                .attachments(attachments)
                .details(details)
                .build();
    }

    public IOReceivedNotification mapToDetails(InternalNotification internalNotification) {
        if(internalNotification == null) return null;


        IOReceivedNotification ioReceivedNotification = modelMapper.map(internalNotification, IOReceivedNotification.class);
        List<NotificationRecipient> filteredNotificationRecipients = ioReceivedNotification.getRecipients()
                .stream()
                .filter(rec -> StringUtils.hasText(rec.getTaxId()))
                .toList();
        if(!CollectionUtils.isEmpty( filteredNotificationRecipients )) {
            ioReceivedNotification.setRecipients( filteredNotificationRecipients );
        }

        // NB: la timeline è GIA' FILTRATA per recipientIndex
        ioReceivedNotification.setCompletedPayments(internalNotification.getTimeline()
                .stream()
                .filter(x -> x.getCategory().equals(TimelineElementCategoryV20.PAYMENT))
                .map(x -> x.getDetails().getNoticeCode())
                .toList());


        return ioReceivedNotification;
    }

    public List<ThirdPartyAttachment> mapToThirdPartyAttachment(List<NotificationDocument> documents, String iun) {
        if(CollectionUtils.isEmpty(documents)) return Collections.emptyList();

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
                .url(URL_ATTACHMENT.replace("{iun}", iun).replace("{indexDocument}", indexDocument + ""))
                .build();
    }
}
