package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV28;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
import it.pagopa.pn.delivery.models.InformalNotificationDetail;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.LegalNotificationDetail;
import it.pagopa.pn.delivery.models.internal.notification.CommunicationType;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

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
    static Converter<it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28,TimelineElementV28> timelineElementConverter =
        context -> {
            it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28 source = context.getSource();
            TimelineElementV28 destination = context.getDestination();

            assert source.getCategory() != null;
            if (destination.getDetails() != null) {
                if(!source.getCategory().equals(TimelineElementCategoryV28.PUBLIC_REGISTRY_VALIDATION_CALL))destination.getDetails().setRecIndexes(null);
                if(!source.getCategory().equals(TimelineElementCategoryV28.NOTIFICATION_CANCELLED)) destination.getDetails().setNotRefinedRecipientIndexes(null);
                if(!source.getCategory().equals(TimelineElementCategoryV28.NOTIFICATION_TIMELINE_REWORKED)) destination.getDetails().setInvalidatedTimelineAndStatusHistory(null);
            }
            destination.setTimestamp(source.getTimestamp());
            return destination;
        };

    static Converter<InformalNotificationRequestV1, InternalNotification> informalNotificationConverter =
        context -> {
            InternalNotification destination = context.getDestination();
            destination.setCommunicationType(CommunicationType.INFORMAL);
            return destination;
        };
    static Converter<NewNotificationRequestV26, InternalNotification> legalNotificationConverter =
        context -> {
            InternalNotification destination = context.getDestination();
            destination.setCommunicationType(CommunicationType.LEGAL);
            return destination;
        };

    static Converter<String, UUID> stringToUuid = ctx ->
            ctx.getSource() != null ? UUID.fromString(ctx.getSource()) : null;


    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy( MatchingStrategies.STRICT );
        modelMapper.createTypeMap( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26.class, NotificationStatusHistoryElementV26 .class )
                .addMapping( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26::getActiveFrom, NotificationStatusHistoryElementV26::setActiveFrom );
        modelMapper.createTypeMap(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28.class, TimelineElementV28.class)
                .setPostConverter(ModelMapperConfig.timelineElementConverter);
        modelMapper.createTypeMap( NotificationRecipient.class, NotificationRecipientEntity.class )
                .addMapping( NotificationRecipient::getTaxId, NotificationRecipientEntity::setRecipientId );
        modelMapper.createTypeMap( NotificationRecipientEntity.class, NotificationRecipient.class )
                .addMapping( NotificationRecipientEntity::getRecipientId, NotificationRecipient::setInternalId );
        modelMapper.createTypeMap(InformalNotificationRequestV1.class, InternalNotification.class)
                .setPostConverter(ModelMapperConfig.informalNotificationConverter);
        modelMapper.createTypeMap(NewNotificationRequestV26.class, InternalNotification.class)
                .setPostConverter(ModelMapperConfig.legalNotificationConverter);
        modelMapper.createTypeMap(NotificationRecipient.class, InformalNotificationRecipientV1.class)
                .addMappings(mapper ->
                        mapper.using(stringToUuid)
                                .map(NotificationRecipient::getMessageId, InformalNotificationRecipientV1::setMessageId)
                );
        modelMapper.createTypeMap(NotificationRecipient.class, FullInformalNotificationRecipientV1.class)
                .addMappings(mapper ->
                        mapper.using(stringToUuid)
                                .map(NotificationRecipient::getMessageId, FullInformalNotificationRecipientV1::setMessageId)
                );
        mapFromLegalNotificationDetailToFullSentNotificationV29(modelMapper);

        mapFromLegalNotificationDetailToFullReceivedNotificationV28(modelMapper);

        mapFromInformalNotificationDetailToFullSentInformalNotificationV1(modelMapper);

        mapFromInformalNotificationDetailToFullReceivedInformalNotificationV1(modelMapper);

        mapFromLegalNotificationDetailToSentNotificationV26(modelMapper);

        mapFromInformalNotificationDetailToInformalSentNotificationV1(modelMapper);

        return modelMapper;
    }

    private static void mapFromInformalNotificationDetailToInformalSentNotificationV1(ModelMapper modelMapper) {
        // InformalNotificationDetail -> InformalSentNotificationV1
        modelMapper.createTypeMap(InformalNotificationDetail.class, InformalSentNotificationV1.class)
                .setProvider(ctx -> modelMapper.map(
                        ((InformalNotificationDetail) ctx.getSource()).getNotification(),
                        InformalSentNotificationV1.class));
    }

    private static void mapFromLegalNotificationDetailToSentNotificationV26(ModelMapper modelMapper) {
        // LegalNotificationDetail -> SentNotificationV26
        modelMapper.createTypeMap(LegalNotificationDetail.class, SentNotificationV26.class)
                .setProvider(ctx -> modelMapper.map(
                        ((LegalNotificationDetail) ctx.getSource()).getNotification(),
                        SentNotificationV26.class));
    }

    private static void mapFromInformalNotificationDetailToFullReceivedInformalNotificationV1(ModelMapper modelMapper) {
        // InformalNotificationDetail -> FullReceivedInformalNotificationV1
        modelMapper.createTypeMap(InformalNotificationDetail.class, FullReceivedInformalNotificationV1.class)
                .setProvider(ctx -> modelMapper.map(
                        ((InformalNotificationDetail) ctx.getSource()).getNotification(),
                        FullReceivedInformalNotificationV1.class))
                .addMappings(mapper -> {
                    mapper.map(InformalNotificationDetail::getTimeline,
                            FullReceivedInformalNotificationV1::setTimeline);
                    mapper.map(InformalNotificationDetail::getNotificationStatus,
                            FullReceivedInformalNotificationV1::setNotificationStatus);
                    mapper.map(InformalNotificationDetail::getNotificationStatusHistory,
                            FullReceivedInformalNotificationV1::setNotificationStatusHistory);
                });
    }

    private static void mapFromInformalNotificationDetailToFullSentInformalNotificationV1(ModelMapper modelMapper) {
        // InformalNotificationDetail -> FullSentInformalNotificationV1
        modelMapper.createTypeMap(InformalNotificationDetail.class, FullSentInformalNotificationV1.class)
                .setProvider(ctx -> modelMapper.map(
                        ((InformalNotificationDetail) ctx.getSource()).getNotification(),
                        FullSentInformalNotificationV1.class))
                .addMappings(mapper -> {
                    mapper.map(InformalNotificationDetail::getTimeline,
                            FullSentInformalNotificationV1::setTimeline);
                    mapper.map(InformalNotificationDetail::getNotificationStatus,
                            FullSentInformalNotificationV1::setNotificationStatus);
                    mapper.map(InformalNotificationDetail::getNotificationStatusHistory,
                            FullSentInformalNotificationV1::setNotificationStatusHistory);
                });
    }

    private static void mapFromLegalNotificationDetailToFullReceivedNotificationV28(ModelMapper modelMapper) {
        // LegalNotificationDetail -> FullReceivedNotificationV28
        modelMapper.createTypeMap(LegalNotificationDetail.class, FullReceivedNotificationV28.class)
                .setProvider(ctx -> modelMapper.map(
                        ((LegalNotificationDetail) ctx.getSource()).getNotification(),
                        FullReceivedNotificationV28.class))
                .addMappings(mapper -> {
                    mapper.map(LegalNotificationDetail::getTimeline,
                            FullReceivedNotificationV28::setTimeline);
                    mapper.map(LegalNotificationDetail::getNotificationStatus,
                            FullReceivedNotificationV28::setNotificationStatus);
                    mapper.map(LegalNotificationDetail::getNotificationStatusHistory,
                            FullReceivedNotificationV28::setNotificationStatusHistory);
                });
    }

    private static void mapFromLegalNotificationDetailToFullSentNotificationV29(ModelMapper modelMapper) {
        modelMapper.createTypeMap(LegalNotificationDetail.class, FullSentNotificationV29.class)
                .setProvider(request -> modelMapper.map(
                        ((LegalNotificationDetail) request.getSource()).getNotification(),
                        FullSentNotificationV29.class
                ))
                .addMappings(mapper -> {
                    mapper.map(LegalNotificationDetail::getTimeline, FullSentNotificationV29::setTimeline);
                    mapper.map(LegalNotificationDetail::getNotificationStatus, FullSentNotificationV29::setNotificationStatus);
                    mapper.map(LegalNotificationDetail::getNotificationStatusHistory, FullSentNotificationV29::setNotificationStatusHistory);
                });
    }


}
