package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequestV21;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV21;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_PHYSICALCOMMUNICATIONTYPE;

@Component
public class EntityToDtoNotificationMapper {

    public InternalNotification entity2Dto(NotificationEntity entity) {
        if ( entity.getPhysicalCommunicationType() == null ) {
            throw new PnInternalException(" Notification entity with iun "
                    + entity.getIun()
                    + " hash invalid physicalCommunicationType value",
                    ERROR_CODE_DELIVERY_UNSUPPORTED_PHYSICALCOMMUNICATIONTYPE);
        }

        List<String> recipientIds = entity.getRecipients().stream().map( NotificationRecipientEntity::getRecipientId )
                .toList();

        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setSenderDenomination(entity.getSenderDenomination());
        internalNotification.set_abstract(entity.getNotificationAbstract());
        internalNotification.setSenderTaxId(entity.getSenderTaxId());
        internalNotification.setNotificationFeePolicy(NotificationFeePolicy.fromValue(entity.getNotificationFeePolicy().getValue()));
        internalNotification.setIun(entity.getIun());
        internalNotification.setSubject(entity.getSubject());
        internalNotification.setSentAt(entity.getSentAt().atOffset(ZoneOffset.UTC));
        internalNotification.setPaProtocolNumber(entity.getPaNotificationId());
        internalNotification.setIdempotenceToken(entity.getIdempotenceToken());
        internalNotification.setCancelledByIun(entity.getCancelledByIun());
        internalNotification.setCancelledIun(entity.getCancelledIun());
        internalNotification.setPhysicalCommunicationType(entity.getPhysicalCommunicationType());
        internalNotification.setGroup(entity.getGroup());
        internalNotification.setSenderPaId(entity.getSenderPaId());
        internalNotification.setRecipients(entity2RecipientsDto(entity.getRecipients()));
        internalNotification.setDocuments(buildDocumentsList(entity));
        internalNotification.setAmount(entity.getAmount());
        internalNotification.setPaymentExpirationDate(entity.getPaymentExpirationDate());
        internalNotification.setTaxonomyCode(entity.getTaxonomyCode());
        internalNotification.setSourceChannel(entity.getSourceChannel());
        internalNotification.setRecipientIds(recipientIds);
        internalNotification.setPaFee(entity.getPaFee());
        internalNotification.setVat(entity.getVat());
        internalNotification.setSourceChannelDetails(entity.getSourceChannelDetails());
        internalNotification.setPagoPaIntMode(entity.getPagoPaIntMode() != null ? NewNotificationRequestV21.PagoPaIntModeEnum.fromValue(entity.getPagoPaIntMode()) : null);
        return internalNotification;
    }

    private List<NotificationRecipient> entity2RecipientsDto(List<NotificationRecipientEntity> recipients) {
        return recipients.stream()
                .map(this::entity2Recipient)
                .toList();
    }

    private NotificationRecipient entity2Recipient(NotificationRecipientEntity entity) {
        return NotificationRecipient.builder()
                .internalId( entity.getRecipientId() )
                .recipientType( NotificationRecipientV21.RecipientTypeEnum.valueOf( entity.getRecipientType().getValue() ) )
                .payments( entity2PaymentInfo( entity.getPayments() ) )
                .build();
    }

    private List<NotificationPaymentInfo> entity2PaymentInfo(List<NotificationPaymentInfoEntity> paymentList) {
        List<NotificationPaymentInfo> notificationPaymentItems = new ArrayList<>();
        if ( !CollectionUtils.isEmpty( paymentList ) ) {
            paymentList.forEach(notificationPaymentInfoEntity -> notificationPaymentItems.add(NotificationPaymentInfo.builder()
                    .f24(entity2PaymentAttachment(notificationPaymentInfoEntity.getF24()))
                    .pagoPa(entity2PaymentAttachment(notificationPaymentInfoEntity))
                    .build()));
        }
        return notificationPaymentItems;
    }

    private F24Payment entity2PaymentAttachment(F24PaymentEntity f24) {
        F24Payment paymentAttachment = null;
        if ( Objects.nonNull( f24 ) ) {
            paymentAttachment = F24Payment.builder()
                    .applyCost(f24.getApplyCost())
                    .index(f24.getIndex())
                    .title(f24.getTitle())
                    .metadataAttachment(
                            MetadataAttachment.builder()
                                    .contentType( f24.getMetadataAttachment().getContentType() )
                                    .digests( NotificationAttachmentDigests.builder()
                                            .sha256( f24.getMetadataAttachment().getNotificationAttachmentDigestsEntity().getSha256() )
                                            .build()
                                    )
                                    .ref( NotificationAttachmentBodyRef.builder()
                                            .key( f24.getMetadataAttachment().getNotificationAttachmentBodyRefEntity().getKey() )
                                            .versionToken( f24.getMetadataAttachment().getNotificationAttachmentBodyRefEntity().getVersionToken() )
                                            .build()
                                    ).build()
                    )
                    .build();
        }
        return paymentAttachment;
    }

    private PagoPaPayment entity2PaymentAttachment(NotificationPaymentInfoEntity pagoPaForm) {
        PagoPaPayment paymentAttachment = null;
        if ( Objects.nonNull( pagoPaForm.getPagoPaForm() ) ) {
            paymentAttachment = PagoPaPayment.builder()
                    .creditorTaxId(pagoPaForm.getCreditorTaxId())
                    .noticeCode(pagoPaForm.getNoticeCode())
                    .applyCost(pagoPaForm.isApplyCost() == null ? true : pagoPaForm.isApplyCost())
                    .attachment(
                            MetadataAttachment.builder()
                                    .contentType( pagoPaForm.getPagoPaForm().getContentType() )
                                    .digests( NotificationAttachmentDigests.builder()
                                            .sha256( pagoPaForm.getPagoPaForm().getNotificationAttachmentDigestsEntity().getSha256() )
                                            .build()
                                    )
                                    .ref( NotificationAttachmentBodyRef.builder()
                                            .key( pagoPaForm.getPagoPaForm().getNotificationAttachmentBodyRefEntity().getKey() )
                                            .versionToken( pagoPaForm.getPagoPaForm().getNotificationAttachmentBodyRefEntity().getVersionToken() )
                                            .build()
                                    ).build()
                    )
                    .build();
        }
        return paymentAttachment;
    }

    /*
    private PagoPaPayment entity2PaymentAttachment(PagoPaPaymentEntity pagoPaForm) {
        PagoPaPayment paymentAttachment = null;
        if ( Objects.nonNull( pagoPaForm ) ) {
            paymentAttachment = PagoPaPayment.builder()
                    .creditorTaxId(pagoPaForm.getCreditorTaxId())
                    .noticeCode(pagoPaForm.getNoticeCode())
                    .applyCost(pagoPaForm.getApplyCost())
                    .attachment(
                            MetadataAttachment.builder()
                                    .contentType( pagoPaForm.getAttachment().getContentType() )
                                    .digests( NotificationAttachmentDigests.builder()
                                            .sha256( pagoPaForm.getAttachment().getNotificationAttachmentDigestsEntity().getSha256() )
                                            .build()
                                    )
                                    .ref( NotificationAttachmentBodyRef.builder()
                                            .key( pagoPaForm.getAttachment().getNotificationAttachmentBodyRefEntity().getKey() )
                                            .versionToken( pagoPaForm.getAttachment().getNotificationAttachmentBodyRefEntity().getVersionToken() )
                                            .build()
                                    ).build()
                    )
                    .build();
        }
        return paymentAttachment;
    }
     */

    private List<NotificationDocument> buildDocumentsList(NotificationEntity entity ) {
        List<NotificationDocument> result = new ArrayList<>();

        if( entity != null && entity.getDocuments() != null) {
            result = entity.getDocuments().stream()
                    .map( this::mapOneDocument )
                    .toList();
        }

        return result;
    }

    private NotificationDocument mapOneDocument(DocumentAttachmentEntity entity ) {
        return entity == null ? null : NotificationDocument.builder()
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
