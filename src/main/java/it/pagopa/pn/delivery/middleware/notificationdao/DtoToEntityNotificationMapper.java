package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.UsedServices;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.NotificationLang;
import it.pagopa.pn.delivery.models.internal.notification.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class DtoToEntityNotificationMapper {

    private static final String IT_LANGUAGE = "IT";

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
                .sourceChannelDetails( dto.getSourceChannelDetails() )
                .paFee(dto.getPaFee())
                .vat(dto.getVat())
                .version( dto.getVersion() )
                .languages( addITLanguageToEntity(dto.getAdditionalLanguages()) )
                .usedServices(dto.getUsedServices() != null ? getUsedServicesEntity(dto.getUsedServices()) : null);

        return builder.build();
    }


    private UsedServicesEntity getUsedServicesEntity(InternalUsedService usedServices) {
        return UsedServicesEntity.builder()
                .physicalAddressLookup(usedServices.getPhysicalAddressLookup())
                .build();
    }

    private List<NotificationLang> addITLanguageToEntity(List<String> additionalLanguages) {
        List<NotificationLang> additionalLanguagesWithIT = new ArrayList<>();
        if(!CollectionUtils.isEmpty(additionalLanguages)){
            additionalLanguagesWithIT.addAll(additionalLanguages.stream()
                    .map(lang -> NotificationLang.builder().lang(lang).build()).toList());
        }
        additionalLanguagesWithIT.add(createITLanguageList());
        return additionalLanguagesWithIT;
    }

    private NotificationLang createITLanguageList() {
        return NotificationLang.builder()
                .lang(IT_LANGUAGE)
                .build();
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
                    paymentInfoEntityList.add(toNotificationPaymentInfoEntity(item)));
        }
        return paymentInfoEntityList;
    }

    private NotificationPaymentInfoEntity toNotificationPaymentInfoEntity(NotificationPaymentInfo item) {
        return NotificationPaymentInfoEntity.builder()
                .creditorTaxId(item.getPagoPa() != null ? item.getPagoPa().getCreditorTaxId() : null)
                .noticeCode(item.getPagoPa() != null ? item.getPagoPa().getNoticeCode() : null)
                .applyCost(item.getPagoPa() != null ? item.getPagoPa().isApplyCost() : null)
                .pagoPaForm(item.getPagoPa() != null ? dto2PagoPaPaymentEntity(item.getPagoPa().getAttachment()) : null)
                .f24(
                        dto2F24PaymentEntity(item.getF24())
                ).build();
    }

    private F24PaymentEntity dto2F24PaymentEntity(F24Payment f24Payment){
        F24PaymentEntity f24PaymentEntity = null;
        if(f24Payment != null){
            f24PaymentEntity =  F24PaymentEntity.builder()
                    .applyCost(f24Payment.isApplyCost())
                    .title(f24Payment.getTitle())
                    .metadataAttachment(dto2PaymentAttachment(f24Payment))
                    .build();
        }
        return f24PaymentEntity;
    }

    private PagoPaPaymentEntity dto2PagoPaPaymentEntity(MetadataAttachment pagoPaPaymentAttachment){
        PagoPaPaymentEntity pagoPaPaymentEntity = null;
        if(pagoPaPaymentAttachment != null){
            pagoPaPaymentEntity = PagoPaPaymentEntity.builder()
                    .contentType(pagoPaPaymentAttachment.getContentType())
                    .ref(NotificationAttachmentBodyRefEntity.builder()
                            .key(pagoPaPaymentAttachment.getRef().getKey())
                            .versionToken(pagoPaPaymentAttachment.getRef().getVersionToken())
                            .build())
                    .digests(NotificationAttachmentDigestsEntity.builder()
                            .sha256(pagoPaPaymentAttachment.getDigests().getSha256())
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
