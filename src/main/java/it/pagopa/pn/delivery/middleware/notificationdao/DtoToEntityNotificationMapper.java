package it.pagopa.pn.delivery.middleware.notificationdao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// FIXME: MapStruct do not play well with lombok. We have to find a solution.
//@Mapper( componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Component
public class DtoToEntityNotificationMapper {

    private final ObjectWriter recipientWriter;

    public DtoToEntityNotificationMapper(ObjectMapper objMapper) {
        this.recipientWriter = objMapper.writerFor(NotificationRecipient.class);
    }

    // FIXME: MapStruct do not play well with lombok. We have to find a solution.
    //@Mapping( target = "iun", source = "iun")
    //@Mapping( target = "paNotificationId", source = "paNotificationId")
    //@Mapping( target = "subject", source = "subject")
    //@Mapping( target = "cancelledIun", source = "cancelledIun")
    //@Mapping( target = "cancelledByIun", source = "cancelledByIun")
    //@Mapping( target = "senderPaId", source = "sender.paId")
    public NotificationEntity dto2Entity(InternalNotification dto) {
        NotificationEntity.NotificationEntityBuilder builder = NotificationEntity.builder()
                .iun( dto.getIun() )
                .paNotificationId( dto.getPaProtocolNumber())
                .senderDenomination( dto.getSenderDenomination() )
                .senderTaxId( dto.getSenderTaxId() )
                .subject( dto.getSubject() )
                .sentAt( dto.getSentAt().toInstant() )
                .cancelledIun( dto.getCancelledIun() )
                .cancelledByIun( dto.getCancelledByIun() )
                .senderPaId( dto.getSenderPaId() )
                .recipientsJson( recipientList2json( dto.getRecipients() ))
                .recipientsOrder( dto.getRecipients().stream()
                        .map( NotificationRecipient::getTaxId )
                        .collect(Collectors.toList())
                    )
                .documentsKeys( listDocumentsKeys( dto.getDocuments() ))
                .documentsDigestsSha256( listDocumentsSha256( dto.getDocuments() ))
                .documentsVersionIds( listDocumentsVersionIds( dto.getDocuments() ))
                .documentsContentTypes( listDocumentsContentTypes( dto.getDocuments() ) )
                .documentsTitles( listDocumentsTitles( dto.getDocuments() ))
                .physicalCommunicationType ( dto.getPhysicalCommunicationType() )
                .notificationFeePolicy( NewNotificationRequest.NotificationFeePolicyEnum.fromValue( dto.getNotificationFeePolicy().getValue() ))
                .group( dto.getGroup() );


        return builder.build();
    }

    private List<String> listDocumentsContentTypes(List<NotificationDocument> documents) {
        return documents.stream()
                .map(NotificationDocument::getContentType)
                .collect( Collectors.toList() );
    }

    private List<String> listDocumentsKeys(List<NotificationDocument> documents) {
        return documents.stream()
                .map( doc -> doc.getRef().getKey() )
                .collect(Collectors.toList());
    }

    private List<String> listDocumentsSha256(List<NotificationDocument> documents) {
        return documents.stream()
                .map( doc -> doc.getDigests().getSha256() )
                .collect(Collectors.toList());
    }

    private List<String> listDocumentsVersionIds(List<NotificationDocument> documents) {
        return documents.stream()
                .map( attachment -> attachment.getRef().getVersionToken() )
                .collect(Collectors.toList());
    }

    private List<String> listDocumentsTitles(List<NotificationDocument> documents) {
        return documents.stream()
                .map(NotificationDocument::getTitle)
                .collect(Collectors.toList());
    }

    private Map<String, String> recipientList2json(List<NotificationRecipient> recipients) {
        Map<String, String> result = new ConcurrentHashMap<>();
        recipients.forEach( recipient ->
            result.put( recipient.getTaxId(), recipient2JsonString( recipient ))
        );
        return result;
    }

    private String recipient2JsonString( NotificationRecipient recipient) {
        try {
            return recipientWriter.writeValueAsString( recipient );
        } catch (JsonProcessingException exc) {
            throw new IllegalStateException( exc );
        }
    }

}
