package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
                .paFee(dto.getPaFee())
                .vat(dto.getVat())
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
                .creditorTaxId(item.getPagoPa() != null ? item.getPagoPa().getCreditorTaxId() : null)
                .noticeCode(item.getPagoPa() != null ? item.getPagoPa().getNoticeCode() : null)
                .applyCost(item.getPagoPa() != null ? item.getPagoPa().isApplyCost() : null)
                .pagoPaForm(
                        dto2PagoPaPaymentEntity(item.getPagoPa())
                )
                .f24(
                        dto2F24PaymentEntity(item.getF24())
                ).build());
        return notificationPaymentInfoEntities;
    }

    private F24PaymentEntity dto2F24PaymentEntity(F24Payment f24Payment){
        F24PaymentEntity f24PaymentEntity = null;
        if(f24Payment != null){
            f24PaymentEntity =  F24PaymentEntity.builder()
                    .applyCost(f24Payment.isApplyCost())
                    .title(f24Payment.getTitle())
                    .index(f24Payment.getIndex())
                    .metadataAttachment(dto2PaymentAttachment(f24Payment))
                    .build();
        }
        return f24PaymentEntity;
    }

    private PagoPaPaymentEntity dto2PagoPaPaymentEntity(PagoPaPayment pagoPaPayment){
        PagoPaPaymentEntity pagoPaPaymentEntity = null;
        if(pagoPaPayment != null){
            pagoPaPaymentEntity = PagoPaPaymentEntity.builder()
                    .contentType(pagoPaPayment.getAttachment().getContentType())
                    .ref(NotificationAttachmentBodyRefEntity.builder()
                            .key(pagoPaPayment.getAttachment().getRef().getKey())
                            .versionToken(pagoPaPayment.getAttachment().getRef().getVersionToken())
                            .build())
                    .digests(NotificationAttachmentDigestsEntity.builder()
                            .sha256(pagoPaPayment.getAttachment().getDigests().getSha256())
                            .build())
                    .build();
        }
        return pagoPaPaymentEntity;
    }

    private MetadataAttachmentEntity dto2PaymentAttachment(F24Payment dto ) {
        MetadataAttachmentEntity pagoPaPaymentEntity = null;
        if (dto != null) {
            pagoPaPaymentEntity = MetadataAttachmentEntity.builder()
                    .ref( NotificationAttachmentBodyRefEntity.builder()
                            .key( dto.getMetadataAttachment().getRef().getKey() )
                            .versionToken( dto.getMetadataAttachment().getRef().getVersionToken() )
                            .build()
                    )
                    .contentType( dto.getMetadataAttachment().getContentType() )
                    .digests( NotificationAttachmentDigestsEntity.builder()
                            .sha256( dto.getMetadataAttachment().getDigests().getSha256() )
                            .build()
                    )
                    .build();
        }
        return pagoPaPaymentEntity;
    }

    /*
    private MetadataAttachmentEntity dto2PaymentAttachment(PagoPaPayment dto ) {
        MetadataAttachmentEntity pagoPaPaymentEntity = null;
        if (dto != null) {
            pagoPaPaymentEntity = MetadataAttachmentEntity.builder()
                    .ref( NotificationAttachmentBodyRefEntity.builder()
                            .key( dto.getAttachment().getRef().getKey() )
                            .versionToken( dto.getAttachment().getRef().getVersionToken() )
                            .build()
                    )
                    .contentType( dto.getAttachment().getContentType() )
                    .digests( NotificationAttachmentDigestsEntity.builder()
                            .sha256( dto.getAttachment().getDigests().getSha256() )
                            .build()
                    )
                    .build();
        }
        return pagoPaPaymentEntity;
    }
     */

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
            entity.setDigests( NotificationAttachmentDigestsEntity.builder()
                    .sha256( dto.getDigests().getSha256() )
                    .build());
            entity.setRef( NotificationAttachmentBodyRefEntity.builder()
                    .key( dto.getRef().getKey() )
                    .versionToken( dto.getRef().getVersionToken() )
                    .build());
        }
        return  entity;
    }


}
