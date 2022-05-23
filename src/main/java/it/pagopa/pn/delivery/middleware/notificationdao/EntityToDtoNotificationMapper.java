package it.pagopa.pn.delivery.middleware.notificationdao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityToDtoNotificationMapper {

    private final ObjectReader recipientReader;
    private ModelMapperFactory modelMapperFactory;

    public EntityToDtoNotificationMapper(ObjectMapper objMapper, ModelMapperFactory modelMapperFactory) {
        this.recipientReader = objMapper.readerFor( NotificationRecipient.class );
        this.modelMapperFactory = modelMapperFactory;
    }

    public InternalNotification entity2Dto(NotificationEntity entity) {
    	if ( entity.getPhysicalCommunicationType() == null ) {
            throw new PnInternalException(" Notification entity with iun " + entity.getIun() + " hash invalid physicalCommunicationType value");
        }

        return new InternalNotification(FullSentNotification.builder()
                .senderDenomination( entity.getSenderDenomination() )
                .senderTaxId( entity.getSenderTaxId() )
                .notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.fromValue( entity.getNotificationFeePolicy().getValue() ))
                .iun( entity.getIun() )
                .subject( entity.getSubject() )
                .sentAt( Date.from(entity.getSentAt()) )
                .paProtocolNumber( entity.getPaNotificationId() )
                .cancelledByIun( entity.getCancelledByIun() )
                .cancelledIun( entity.getCancelledIun() )
                .physicalCommunicationType( entity.getPhysicalCommunicationType() )
                .group( entity.getGroup() )
                .senderPaId( entity.getSenderPaId() )
                .recipients( entity2RecipientDto( entity.getRecipients() ) )
                //.documentsAvailable(  )
                .build()
        , Collections.emptyMap());
    }

    private List<NotificationRecipient> entity2RecipientDto(List<NotificationRecipientEntity> recipients) {
        ModelMapper mapper = modelMapperFactory.createModelMapper( NotificationRecipientEntity.class, NotificationRecipient.class );
        return recipients.stream().map( r ->  mapper.map( r, NotificationRecipient.class ) ).collect(Collectors.toList());
    }
}
