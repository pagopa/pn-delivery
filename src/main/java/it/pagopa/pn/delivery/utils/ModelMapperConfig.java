package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV27;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {
    
    static Converter<it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27,TimelineElementV27> timelineElementV27Converter =
        context -> {
            it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27 source = context.getSource();
            TimelineElementV27 destination = context.getDestination();
            assert source.getCategory() != null;
            /*
                Mapping per settare a null i campi recIndexes e notRefinedRecipients per tutti gli elementi di timeline,
                eccetto per quelli che li utilizzano effettivamente.
            */
            if(!source.getCategory().equals(TimelineElementCategoryV27.PUBLIC_REGISTRY_VALIDATION_CALL))destination.getDetails().setRecIndexes(null);
            if(!source.getCategory().equals(TimelineElementCategoryV27.NOTIFICATION_CANCELLED)) destination.getDetails().setNotRefinedRecipientIndexes(null);

            destination.setTimestamp(source.getTimestamp());
            return destination;
        };

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy( MatchingStrategies.STRICT );
        modelMapper.createTypeMap( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26.class, NotificationStatusHistoryElementV26 .class )
                .addMapping( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26::getActiveFrom, NotificationStatusHistoryElementV26::setActiveFrom );
        modelMapper.createTypeMap(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27.class,
                TimelineElementV27.class)
                .setPostConverter(ModelMapperConfig.timelineElementV27Converter);
        modelMapper.createTypeMap( NotificationRecipient.class, NotificationRecipientEntity.class )
                .addMapping( NotificationRecipient::getTaxId, NotificationRecipientEntity::setRecipientId );
        modelMapper.createTypeMap( NotificationRecipientEntity.class, NotificationRecipient.class )
                .addMapping( NotificationRecipientEntity::getRecipientId, NotificationRecipient::setInternalId );
        return modelMapper;
    }

}
