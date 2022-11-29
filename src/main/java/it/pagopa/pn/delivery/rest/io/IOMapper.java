package it.pagopa.pn.delivery.rest.io;

import it.pagopa.pn.delivery.generated.openapi.appio.v1.dto.IOReceivedNotification;
import it.pagopa.pn.delivery.generated.openapi.appio.v1.dto.ThirdPartyAttachment;
import it.pagopa.pn.delivery.generated.openapi.appio.v1.dto.ThirdPartyMessage;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDocument;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchResponse;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class IOMapper {

    private static final String URL_ATTACHMENT = "/delivery/notifications/sent/{iun}/attachments/documents/{indexDocument}";

    private final ModelMapperFactory modelMapperFactory;

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
        ModelMapper mapper = modelMapperFactory.createModelMapper(ResultPaginationDto.class, NotificationSearchResponse.class );
        return mapper.map( internalNotification, IOReceivedNotification.class );
    }

    public List<ThirdPartyAttachment> mapToThirdPartyAttachment(List<NotificationDocument> documents, String iun) {
        if(CollectionUtils.isEmpty(documents)) return Collections.emptyList();

        return IntStream
                .range(0, documents.size())
                .mapToObj(index -> mapToThirdPartyAttachment(documents.get(index), index, iun))
                .collect(Collectors.toList());
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
