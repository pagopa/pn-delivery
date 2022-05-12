package it.pagopa.pn.delivery.middleware.notificationdao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//@Mapper( componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Component
public class EntityToDtoNotificationMapper {

    private final ObjectReader recipientReader;

    public EntityToDtoNotificationMapper(ObjectMapper objMapper) {
        this.recipientReader = objMapper.readerFor( NotificationRecipient.class );
    }

    public InternalNotification entity2Dto(NotificationEntity entity) {
    	if ( entity.getPhysicalCommunicationType() == null ) {
            throw new PnInternalException(" Notification entity with iun " + entity.getIun() + " hash invalid physicalCommunicationType value");
        }

        return new InternalNotification(FullSentNotification.builder()
                .iun( entity.getIun() )
                .subject( entity.getSubject() )
                .sentAt( Date.from(entity.getSentAt()) )
                .paProtocolNumber( entity.getPaNotificationId() )
                .cancelledByIun( entity.getCancelledByIun() )
                .cancelledIun( entity.getCancelledIun() )
                .physicalCommunicationType( entity.getPhysicalCommunicationType() )
                .group( entity.getGroup() )
                .senderPaId( entity.getSenderPaId() )
                .recipients( buildRecipientsList( entity ) )
                .documents( buildDocumentsList( entity ) )
                .build()
        , Collections.EMPTY_MAP);
    }

    private NotificationAttachment buildAttachment(String key, String version, String sha256 ) {
        NotificationAttachment result;
        if ( StringUtils.isAllBlank( key, version, sha256 ) ) {
            result = null;
        }
        else if ( version != null && StringUtils.isNotBlank( sha256 ) && StringUtils.isNotBlank( key ) ) {
            result = NotificationAttachment.builder()
                    .ref( NotificationAttachmentBodyRef.builder()
                            .key( key )
                            .versionToken( version )
                            .build()
                    )
                    .digests( NotificationAttachmentDigests.builder()
                            .sha256( sha256 )
                            .build()
                    )
                    .build();
        }
        else {
            throw new PnInternalException( "Error key (" + key + ") version (" + version + ") and sha256 (" + sha256 + ") are both required or both blank" );
        }
        return result;
    }

    private NotificationDocument buildDocument(String key, String version, String sha256, String contentType, String title ) {
        return NotificationDocument.builder()
                .title( title )
                .ref(NotificationAttachmentBodyRef.builder()
                        .versionToken( version )
                        .key( key )
                        .build())
                .digests( NotificationAttachmentDigests.builder()
                        .sha256( sha256 )
                        .build() )
                .contentType( contentType )
                .build();
    }

    private List<NotificationDocument> buildDocumentsList(NotificationEntity entity ) {
        List<String> documentsDigestsSha256 = entity.getDocumentsDigestsSha256();
        List<String> documentsKeys = entity.getDocumentsKeys();
        List<String> documentsVersionIds = entity.getDocumentsVersionIds();
        List<String> documentsContentTypes = entity.getDocumentsContentTypes();
        List<String> documentsTitles = entity.getDocumentsTitles();

        int lengthShas = nullSafeGetLength(documentsDigestsSha256);
        int lengthKeys = nullSafeGetLength(documentsKeys);
        int lengthVersionIds = nullSafeGetLength(documentsVersionIds);
        int lengthContentTypes = nullSafeGetLength(documentsContentTypes);
        int lengthTitles = nullSafeGetLength(documentsTitles);
        if (areAllEquals(lengthShas, lengthKeys, lengthVersionIds, lengthContentTypes, lengthTitles)) {
            throw new PnInternalException(" Notification entity with iun " + entity.getIun() +
                    " hash different quantity of document versions, sha256s, keys, content types and titles");
        }

        // - Three different list with one information each instead of a list of object:
        //   AWS keyspace do not support UDT
        List<NotificationDocument> result = new ArrayList<>();
        for( int d = 0; d < lengthShas; d += 1 ) {
            NotificationDocument document = buildDocument(
                    documentsKeys.get( d ),
                    documentsVersionIds.get( d ),
                    documentsDigestsSha256.get( d ),
                    documentsContentTypes.get( d ),
                    documentsTitles.get( d )
                );
            result.add( document );
        }

        return result;
    }

    private boolean areAllEquals(int lengthShas, int lengthKeys, int lengthVersionIds, int lengthContentTypes, int lengthTitles) {
        return lengthShas != lengthKeys
                || lengthKeys != lengthVersionIds
                || lengthVersionIds != lengthContentTypes
                || lengthTitles != lengthContentTypes;
    }

    private int nullSafeGetLength(List<String> documentsDigestsSha256) {
        return documentsDigestsSha256 == null ? 0 : documentsDigestsSha256.size();
    }

    private List<NotificationRecipient> buildRecipientsList( NotificationEntity entity ) {
        Map<String, String> recipientsMetadata = entity.getRecipientsJson();

        return entity.getRecipientsOrder().stream()
                .map( recipientId -> parseRecipientJson( recipientsMetadata.get( recipientId)) )
                .collect(Collectors.toList());

    }

    private NotificationRecipient parseRecipientJson( String jsonString ) {
        try {
            return this.recipientReader.readValue( jsonString );
        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Parsing cassandra stored json", exc );
        }
    }
}
