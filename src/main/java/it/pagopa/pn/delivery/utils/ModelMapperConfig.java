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
    /*
        Dopo la migrazione di delivery-push a Spring Boot 3, l'API di history ha modificato il formato delle risposte JSON:
        prima restituiva tutti i possibili campi di details (principalmente valorizzati a null),
        mentre ora restituisce solo i campi effettivamente utilizzati.
        Questo cambiamento impatta due campi specifici (recIndexes e notRefinedRecipients) che il generatore di questo microservizio istanzia
        di default come liste vuote. (Questo perchè i 2 campi sono definiti come required nei rispettivi schemi OpenAPI).
        Poiché questi campi non sono più presenti nella risposta, vengono automaticamente valorizzati per tutti gli elementi di timeline come
        liste vuote invece che come null, alterando il comportamento originale del servizio.
        Per mantenere la coerenza con i client (che si aspettano null), questo mapping imposta a null i campi recIndexes e notRefinedRecipients
        per tutti gli elementi della timeline, eccetto quelli che li utilizzano effettivamente.
        Questo workaround sarà necessario fino al completamento della migrazione a Spring Boot 3 e all'aggiornamento del plugin di generazione.
     */
    static Converter<it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27,TimelineElementV27> timelineElementV27Converter =
        context -> {
            it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27 source = context.getSource();
            TimelineElementV27 destination = context.getDestination();

            assert source.getCategory() != null;
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
