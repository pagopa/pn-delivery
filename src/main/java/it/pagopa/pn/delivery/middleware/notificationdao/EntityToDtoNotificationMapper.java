package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.DocumentAttachmentEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityToDtoNotificationMapper {

    public InternalNotification entity2Dto(NotificationEntity entity) {
    	if ( entity.getPhysicalCommunicationType() == null ) {
            throw new PnInternalException(" Notification entity with iun " + entity.getIun() + " hash invalid physicalCommunicationType value");
        }

    	List<String> recipientIds = entity.getRecipients().stream().map( NotificationRecipientEntity::getRecipientId )
                .collect(Collectors.toList());

        return new InternalNotification(FullSentNotification.builder()
                .senderDenomination( entity.getSenderDenomination() )
                ._abstract( entity.get_abstract() )
                .senderTaxId( entity.getSenderTaxId() )
                .notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.fromValue( entity.getNotificationFeePolicy().getValue() ))
                .iun( entity.getIun() )
                .subject( entity.getSubject() )
                .sentAt( entity.getSentAt().atOffset( ZoneOffset.UTC ) )
                .paProtocolNumber( entity.getPaNotificationId() )
                .idempotenceToken( entity.getIdempotenceToken() )
                .cancelledByIun( entity.getCancelledByIun() )
                .cancelledIun( entity.getCancelledIun() )
                .physicalCommunicationType( entity.getPhysicalCommunicationType() )
                .group( entity.getGroup() )
                .senderPaId( entity.getSenderPaId() )
                .recipients( entity2RecipientDto( entity.getRecipients() ) )
                .documents( buildDocumentsList( entity ) )
                .amount(entity.getAmount())
                .paymentExpirationDate(entity.getPaymentExpirationDate())
                .taxonomyCode( entity.getTaxonomyCode() )
                .build()
        , Collections.emptyMap(), recipientIds );
    }

    private List<NotificationRecipient> entity2RecipientDto(List<NotificationRecipientEntity> recipients) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        mapper.createTypeMap( NotificationRecipientEntity.class, NotificationRecipient.class )
                .addMapping( NotificationRecipientEntity::getRecipientId, NotificationRecipient::setInternalId );

        return recipients.stream()
                .map( r -> mapper.map(r, NotificationRecipient.class))
                .collect(Collectors.toList());
    }



    private List<NotificationDocument> buildDocumentsList(NotificationEntity entity ) {
        List<NotificationDocument> result = new ArrayList<>();

        if( entity != null && entity.getDocuments() != null) {
            result = entity.getDocuments().stream()
                    .map( this::mapOneDocument )
                    .collect(Collectors.toList());
        }

        return result;
    }

    private NotificationDocument mapOneDocument(DocumentAttachmentEntity entity ) {
        return entity == null ? null : NotificationDocument.builder()
                .requiresAck( entity.getRequiresAck() )
                .sendByMail( entity.getSendByMail() )
                .title( entity.getTitle() )
                .ref(NotificationAttachmentBodyRef.builder()
                        .versionToken( entity.getRef().getVersionToken() )
                        .key( entity.getRef().getKey() )
                        .build())
                .digests( NotificationAttachmentDigests.builder()
                        .sha256( entity.getDigests().getSha256() )
                        .build() )
                .contentType( entity.getContentType() )
                .build();
    }
}
