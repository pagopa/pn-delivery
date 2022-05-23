package it.pagopa.pn.delivery.middleware.notificationdao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDocument;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DtoToEntityNotificationMapper {

    private final ObjectWriter recipientWriter;
    private ModelMapperFactory modelMapperFactory;

    public DtoToEntityNotificationMapper(ObjectMapper objMapper, ModelMapperFactory modelMapperFactory) {
        this.recipientWriter = objMapper.writerFor(NotificationRecipient.class);
        this.modelMapperFactory = modelMapperFactory;
    }

    public NotificationEntity dto2Entity(InternalNotification dto) {
        NotificationEntity.NotificationEntityBuilder builder = NotificationEntity.builder()
                .iun( dto.getIun() )
                .paNotificationId( dto.getPaProtocolNumber())
                .senderDenomination( dto.getSenderDenomination() )
                .senderTaxId( dto.getSenderTaxId() )
                .subject( dto.getSubject() )
                .sentAt( dto.getSentAt().toInstant() )
                .cancelledIun( dto.getCancelledIun() )
                .cancelledByIun( dto.getCancelledByIun() )
                .senderPaId( dto.getSenderPaId() )
                .recipients( dto2RecipientsEntity( dto.getRecipients() ) )
                .recipientIds( dto.getRecipients().stream()
                        .map( NotificationRecipient::getTaxId )
                        .collect(Collectors.toList())
                )
                .physicalCommunicationType ( dto.getPhysicalCommunicationType() )
                .notificationFeePolicy( NewNotificationRequest.NotificationFeePolicyEnum.fromValue( dto.getNotificationFeePolicy().getValue() ))
                .group( dto.getGroup() );

        return builder.build();
    }

    private List<NotificationRecipientEntity> dto2RecipientsEntity(List<NotificationRecipient> recipients) {
       ModelMapper mapper = modelMapperFactory.createModelMapper( NotificationRecipient.class, NotificationRecipientEntity.class );
       return recipients.stream().map( r ->  mapper.map( r, NotificationRecipientEntity.class )).collect(Collectors.toList());
    }

}
