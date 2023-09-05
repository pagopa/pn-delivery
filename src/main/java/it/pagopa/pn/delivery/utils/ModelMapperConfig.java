package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV21;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
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
        modelMapper.createTypeMap( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElement.class, NotificationStatusHistoryElement .class )
                .addMapping( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElement::getActiveFrom, NotificationStatusHistoryElement::setActiveFrom );
        modelMapper.createTypeMap( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElement.class, TimelineElement.class )
                .addMapping(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElement::getTimestamp, TimelineElement::setTimestamp );
        modelMapper.createTypeMap( NotificationRecipientV21.class, NotificationRecipientEntity.class )
                .addMapping( NotificationRecipientV21::getTaxId, NotificationRecipientEntity::setRecipientId );
        modelMapper.createTypeMap( NotificationRecipientEntity.class, NotificationRecipientV21.class )
                .addMapping( NotificationRecipientEntity::getRecipientId, NotificationRecipientV21::setInternalId );
        return modelMapper;
    }

}
