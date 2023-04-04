package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientPrivate;
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
        modelMapper.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, NotificationStatusHistoryElement .class )
                .addMapping( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement::getActiveFrom, NotificationStatusHistoryElement::setActiveFrom );
        modelMapper.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class )
                .addMapping(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement::getTimestamp, TimelineElement::setTimestamp );
        modelMapper.createTypeMap( NotificationRecipient.class, NotificationRecipientEntity.class )
                .addMapping( NotificationRecipient::getTaxId, NotificationRecipientEntity::setRecipientId );
        modelMapper.createTypeMap( NotificationRecipientPrivate.class, NotificationRecipientEntity.class )
                .addMapping( NotificationRecipientPrivate::getTaxId, NotificationRecipientEntity::setRecipientId );
        modelMapper.createTypeMap( NotificationRecipientEntity.class, NotificationRecipientPrivate.class )
                .addMapping( NotificationRecipientEntity::getRecipientId, NotificationRecipientPrivate::setInternalId );
        return modelMapper;
    }

}
