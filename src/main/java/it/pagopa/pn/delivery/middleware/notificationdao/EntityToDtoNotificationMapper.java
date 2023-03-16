package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
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

        return new InternalNotification(FullSentNotification.builder()
                .senderDenomination( entity.getSenderDenomination() )
                ._abstract( entity.getNotificationAbstract() )
                .senderTaxId( entity.getSenderTaxId() )
                .notificationFeePolicy( NotificationFeePolicy.fromValue( entity.getNotificationFeePolicy().getValue() ))
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
                .recipients( entity2RecipientsDto( entity.getRecipients() ) )
                .documents( buildDocumentsList( entity ) )
                .amount(entity.getAmount())
                .paymentExpirationDate(entity.getPaymentExpirationDate())
                .taxonomyCode( entity.getTaxonomyCode() )
                .build(),
                recipientIds, entity.getSourceChannel()
        );
    }

    private List<NotificationRecipient> entity2RecipientsDto(List<NotificationRecipientEntity> recipients) {
        return recipients.stream()
                .map(this::entity2Recipient)
                .toList();
    }

    private NotificationRecipient entity2Recipient(NotificationRecipientEntity entity) {
        return NotificationRecipient.builder()
                .internalId( entity.getRecipientId() )
                .recipientType( NotificationRecipient.RecipientTypeEnum.valueOf( entity.getRecipientType().getValue() ) )
                .payment( entity2PaymentInfo( entity.getPaymentList() ) )
                .build();
    }

    private NotificationPaymentInfo entity2PaymentInfo(List<NotificationPaymentInfoEntity> paymentList) {
        NotificationPaymentInfo notificationPaymentInfo = null;
        if ( !CollectionUtils.isEmpty( paymentList ) ) {
            notificationPaymentInfo = NotificationPaymentInfo.builder()
                    .creditorTaxId( paymentList.get( 0 ).getCreditorTaxId() )
                    .noticeCode( paymentList.get( 0 ).getNoticeCode() )
                    .noticeCodeAlternative( paymentList.size() > 1 ? paymentList.get( 1 ).getNoticeCode() : null )
                    .pagoPaForm( entity2PaymentAttachment( paymentList.get( 0 ).getPagoPaForm() ) )
                    .build();
        }
        return notificationPaymentInfo;
    }

    private NotificationPaymentAttachment entity2PaymentAttachment(PaymentAttachmentEntity pagoPaForm) {
        NotificationPaymentAttachment paymentAttachment = null;
        if ( Objects.nonNull( pagoPaForm ) ) {
            paymentAttachment = NotificationPaymentAttachment.builder()
                    .contentType( pagoPaForm.getContentType() )
                    .digests( NotificationAttachmentDigests.builder()
                            .sha256( pagoPaForm.getDigests().getSha256() )
                            .build()
                    )
                    .ref( NotificationAttachmentBodyRef.builder()
                            .key( pagoPaForm.getRef().getKey() )
                            .versionToken( pagoPaForm.getRef().getVersionToken() )
                            .build()
                    )
                    .build();
        }
        return paymentAttachment;
    }

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
