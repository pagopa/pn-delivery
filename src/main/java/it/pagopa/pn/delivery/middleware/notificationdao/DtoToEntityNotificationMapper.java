package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class DtoToEntityNotificationMapper {
    public static final int NOTIFICATION_VERSION = 1;

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
                .sourceChannel( dto.getSourceChannel() )
                .version( NOTIFICATION_VERSION );

        return builder.build();
    }

    private List<NotificationRecipientEntity> dto2RecipientsEntity(
            List<NotificationRecipient> recipients
    ) {
        return recipients.stream()
                .map( this::dto2RecipientEntity )
                .toList();
    }

    private NotificationRecipientEntity dto2RecipientEntity( NotificationRecipient recipient ) {
        return NotificationRecipientEntity.builder()
                .recipientId( recipient.getTaxId() )
                .recipientType( RecipientTypeEntity.valueOf( recipient.getRecipientType().getValue() ) )
                .paymentList( dto2PaymentList( recipient.getPayment() ) )
                .build();
    }

    private List<NotificationPaymentInfoEntity> dto2PaymentList(NotificationPaymentInfo dto) {
        List<NotificationPaymentInfoEntity> paymentInfoEntityList = null;
        if ( dto != null) {
            paymentInfoEntityList = new ArrayList<>();
            paymentInfoEntityList.add( NotificationPaymentInfoEntity.builder()
                    .creditorTaxId( dto.getCreditorTaxId() )
                    .noticeCode( dto.getNoticeCode() )
                    .pagoPaForm( dto2PaymentAttachment( dto.getPagoPaForm() ) )
                    .build()
            );
            if ( StringUtils.hasText( dto.getNoticeCodeAlternative() ) ) {
                paymentInfoEntityList.add( NotificationPaymentInfoEntity.builder()
                        .creditorTaxId( dto.getCreditorTaxId() )
                        .noticeCode( dto.getNoticeCodeAlternative() )
                        .pagoPaForm( dto2PaymentAttachment( dto.getPagoPaForm() ) )
                        .build()
                );
            }
        }
        return paymentInfoEntityList;
    }

    private PaymentAttachmentEntity dto2PaymentAttachment( NotificationPaymentAttachment dto ) {
        PaymentAttachmentEntity paymentAttachmentEntity = null;
        if (dto != null) {
            paymentAttachmentEntity = PaymentAttachmentEntity.builder()
                    .ref( AttachmentRefEntity.builder()
                            .key( dto.getRef().getKey() )
                            .versionToken( dto.getRef().getVersionToken() )
                            .build()
                    )
                    .contentType( dto.getContentType() )
                    .digests( AttachmentDigestsEntity.builder()
                            .sha256( dto.getDigests().getSha256() )
                            .build()
                    )
                    .build();
        }
        return paymentAttachmentEntity;
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
