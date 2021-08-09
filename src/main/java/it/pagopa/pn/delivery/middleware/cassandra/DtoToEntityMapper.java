package it.pagopa.pn.delivery.middleware.cassandra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationPaymentInfo;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.delivery.model.notification.cassandra.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

//@Mapper( componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Component
public class DtoToEntityMapper {

    private final ObjectMapper objMapper;
    private final ObjectWriter recipientWriter;

    public DtoToEntityMapper(ObjectMapper objMapper) {
        this.objMapper = objMapper;
        this.recipientWriter = this.objMapper.writerFor(NotificationRecipient.class);
    }

    //@Mapping( target = "iun", source = "iun")
    //@Mapping( target = "paNotificationId", source = "paNotificationId")
    //@Mapping( target = "subject", source = "subject")
    //@Mapping( target = "cancelledIun", source = "cancelledIun")
    //@Mapping( target = "cancelledByIun", source = "cancelledByIun")
    //@Mapping( target = "senderPaId", source = "sender.paId")
    public NotificationEntity dto2Entity(Notification dto) {
        NotificationEntity.NotificationEntityBuilder builder = NotificationEntity.builder()
                .iun( dto.getIun() )
                .paNotificationId( dto.getPaNotificationId())
                .subject( dto.getSubject() )
                .cancelledIun( dto.getCancelledIun() )
                .cancelledByIun( dto.getCancelledByIun() )
                .senderPaId( dto.getSender().getPaId() )
                .recipientsJson( recipientList2json( dto.getRecipients() ))
                .documentsDigestsSha256( listDocumentsSha256( dto.getDocuments() ))
            ;



        NotificationPaymentInfo paymentInfo = dto.getPayment();
        fillBuilderWithPaymentInfo(builder, paymentInfo);

        return builder.build();
    }

    private List<String> listDocumentsSha256(List<NotificationAttachment> documents) {
        return documents.stream()
                .map( doc -> doc.getDigests().getSha256() )
                .collect(Collectors.toList());
    }

    private void fillBuilderWithPaymentInfo(NotificationEntity.NotificationEntityBuilder builder, NotificationPaymentInfo paymentInfo) {
        if( paymentInfo != null ) {
            builder
                .iuv( paymentInfo.getIuv() )
                .notificationFeePolicy( paymentInfo.getNotificationFeePolicy() );

            if( paymentInfo.getF24() != null ) {

                NotificationAttachment flatRateF24 = paymentInfo.getF24().getFlatRate();
                if( flatRateF24 != null ) {
                    builder.f24FlatRateDigestSha256( flatRateF24.getDigests().getSha256() );
                }

                NotificationAttachment digitalF24 = paymentInfo.getF24().getDigital();
                if( digitalF24 != null ) {
                    builder.f24DigitalDigestSha256( digitalF24.getDigests().getSha256() );
                }

                NotificationAttachment analogF24 = paymentInfo.getF24().getAnalog();
                if( analogF24 != null ) {
                    builder.f24AnalogDigestSha256( analogF24.getDigests().getSha256() );
                }
            }
        }
    }

    private Map<String, String> recipientList2json(List<NotificationRecipient> recipients) {
        Map<String, String> result = new ConcurrentHashMap<>();
        recipients.forEach( recipient -> {
            result.put( recipient.getFc(), recipient2JsonString( recipient ));
        });
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
