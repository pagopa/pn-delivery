package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV26;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {


    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy( MatchingStrategies.STRICT );
        modelMapper.createTypeMap( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26.class, NotificationStatusHistoryElementV26 .class )
                .addMapping( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26::getActiveFrom, NotificationStatusHistoryElementV26::setActiveFrom );
        modelMapper.createTypeMap( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV26.class, TimelineElementV26.class )
                .addMapping(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV26::getTimestamp, TimelineElementV26::setTimestamp );
        modelMapper.createTypeMap( NotificationRecipient.class, NotificationRecipientEntity.class )
                .addMapping( NotificationRecipient::getTaxId, NotificationRecipientEntity::setRecipientId );
        modelMapper.createTypeMap( NotificationRecipientEntity.class, NotificationRecipient.class )
                .addMapping( NotificationRecipientEntity::getRecipientId, NotificationRecipient::setInternalId );
        return modelMapper;
    }

}
