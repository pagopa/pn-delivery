package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDocument;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DtoToEntityNotificationMapper {

    public NotificationEntity dto2Entity(InternalNotification dto) {
        NotificationEntity.NotificationEntityBuilder builder = NotificationEntity.builder()
                .iun( dto.getIun() )
                .notificationAbstract( dto.getAbstract() )
                .idempotenceToken( dto.getIdempotenceToken() )
                .paNotificationId( dto.getPaProtocolNumber())
                .senderDenomination( dto.getSenderDenomination() )
                .senderTaxId( dto.getSenderTaxId() )
                .subject( dto.getSubject() )
                .sentAt( dto.getSentAt().toInstant() )
                .cancelledIun( dto.getCancelledIun() )
                .cancelledByIun( dto.getCancelledByIun() )
                .senderPaId( dto.getSenderPaId() )
                .recipients( dto2RecipientsEntity( dto.getRecipients() ) )
                .documents( convertDocuments( dto.getDocuments() ))
                .physicalCommunicationType ( dto.getPhysicalCommunicationType() )
                .notificationFeePolicy( NotificationFeePolicy.fromValue( dto.getNotificationFeePolicy().getValue() ))
                .group( dto.getGroup() )
                .amount(dto.getAmount())
                .paymentExpirationDate(dto.getPaymentExpirationDate())
                .taxonomyCode(dto.getTaxonomyCode())
                .sourceChannel( dto.getSourceChannel() );

        return builder.build();
    }

    private List<NotificationRecipientEntity> dto2RecipientsEntity(
            List<NotificationRecipient> recipients
    ) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        mapper.createTypeMap( NotificationRecipient.class, NotificationRecipientEntity.class )
                .addMapping( NotificationRecipient::getTaxId, NotificationRecipientEntity::setRecipientId );

        return recipients.stream()
               .map( r -> mapper.map( r, NotificationRecipientEntity.class ))
               .toList();
    }

    private List<DocumentAttachmentEntity> convertDocuments(List<NotificationDocument> dtoList) {
        List<DocumentAttachmentEntity> entityList = null;
        if( dtoList != null ) {
            entityList = dtoList.stream().map(this::convertDocument).toList();
        }
        return entityList;
    }

    private DocumentAttachmentEntity convertDocument( NotificationDocument dto ) {
        DocumentAttachmentEntity entity = null;
        if( dto != null ) {
            entity = new DocumentAttachmentEntity();
            entity.setContentType( dto.getContentType() );
            entity.setTitle( dto.getTitle() );
            entity.setDigests( AttachmentDigestsEntity.builder()
                    .sha256( dto.getDigests().getSha256() )
                    .build());
            entity.setRef( AttachmentRefEntity.builder()
                    .key( dto.getRef().getKey() )
                    .versionToken( dto.getRef().getVersionToken() )
                    .build());
        }
        return  entity;
    }


}
