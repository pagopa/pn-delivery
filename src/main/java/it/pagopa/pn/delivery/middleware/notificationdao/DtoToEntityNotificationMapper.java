package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
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
                .pagoPaIntMode( dto.getPagoPaIntMode().getValue() )
                .sourceChannel( dto.getSourceChannel() )
                .sourceChannelDetails( dto.getSourceChannel() )
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
                .payments( dto2PaymentList( recipient.getPayments() ) )
                .build();
    }

    private List<NotificationPaymentInfoEntity> dto2PaymentList(List<NotificationPaymentInfo> notificationPaymentInfos) {
        List<NotificationPaymentInfoEntity> paymentInfoEntityList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(notificationPaymentInfos)) {
            notificationPaymentInfos.forEach(item ->
                    paymentInfoEntityList.addAll(toNotificationPaymentInfoEntityList(item)));
        }
        return paymentInfoEntityList;
    }

    private List<NotificationPaymentInfoEntity> toNotificationPaymentInfoEntityList(NotificationPaymentInfo item) {
        List<NotificationPaymentInfoEntity> notificationPaymentInfoEntities = new ArrayList<>();
        notificationPaymentInfoEntities.add(NotificationPaymentInfoEntity.builder()
                .creditorTaxId( item.getCreditorTaxId() )
                .noticeCode( item.getNoticeCode() )
                .pagoPaForm( dto2PaymentAttachment( item.getPagoPaForm() ) )
                .f24(dto2PaymentAttachment(item.getF24()))
                .build());

        if ( StringUtils.hasText( item.getNoticeCodeAlternative() ) ) {
            notificationPaymentInfoEntities.add( NotificationPaymentInfoEntity.builder()
                    .creditorTaxId( item.getCreditorTaxId() )
                    .noticeCode( item.getNoticeCodeAlternative() )
                    .pagoPaForm( dto2PaymentAttachment( item.getPagoPaForm() ) )
                    .f24(dto2PaymentAttachment(item.getF24()))
                    .build()
            );
        }
        return notificationPaymentInfoEntities;
    }


    private PaymentAttachmentEntity dto2PaymentAttachment( F24Payment dto ) {
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
