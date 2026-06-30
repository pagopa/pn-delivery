package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV28;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
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

    static Converter<String, UUID> stringToUuid = ctx ->
            ctx.getSource() != null ? UUID.fromString(ctx.getSource()) : null;

    /**
     * Converte la riga di ricerca interna nel DTO per il destinatario:
     * <ul>
     *     <li>{@code communicationType}: valore dell'entity, con default {@code LEGAL} quando assente
     *     (retrocompatibilità con le notifiche storiche prive del campo);</li>
     *     <li>{@code communicationOutcomes}: assemblato dai campi flat {@code viewed}/{@code delivered}
     *     e valorizzato solo quando almeno uno dei due è presente (tipicamente le comunicazioni bonarie);</li>
     *     <li>{@code desiredFeedback}: volutamente NON mappato in output.</li>
     * </ul>
     */
    static Converter<it.pagopa.pn.delivery.models.NotificationSearchRow, FullNotificationSearchRow> recipientSearchRowConverter =
        context -> {
            it.pagopa.pn.delivery.models.NotificationSearchRow source = context.getSource();
            FullNotificationSearchRow destination = context.getDestination();

            destination.setCommunicationType(
                    org.springframework.util.StringUtils.hasText(source.getCommunicationType())
                            ? FullNotificationSearchRow.CommunicationTypeEnum.fromValue(source.getCommunicationType())
                            : FullNotificationSearchRow.CommunicationTypeEnum.LEGAL);

            if (source.getViewed() != null || source.getDelivered() != null) {
                CommunicationOutcomes outcomes = new CommunicationOutcomes();
                outcomes.setViewed(source.getViewed());
                outcomes.setDelivered(source.getDelivered());
                destination.setCommunicationOutcomes(outcomes);
            }
            return destination;
        };

    /**
     * Converte la riga di ricerca interna nel DTO per il flusso bonario (campagna):
     * <ul>
     *     <li>{@code communicationType}: valore dell'entity, con default {@code INFORMAL} quando
     *     assente (per costruzione il flusso campagna ricerca solo comunicazioni bonarie);</li>
     *     <li>{@code communicationOutcomes}: assemblato dai campi flat {@code viewed}/{@code delivered}
     *     e valorizzato solo quando almeno uno dei due è presente;</li>
     *     <li>{@code notificationStatus}: lasciato alla mappatura implicita STRICT che converte per nome
     *     da {@code UnifiedNotificationStatus} a {@code InformalNotificationStatus} (la compatibilità
     *     è garantita a monte dal fail-fast di {@code InformalNotificationStatusValidator});</li>
     *     <li>{@code desiredFeedback}: volutamente NON mappato in output (resta solo su DB).</li>
     * </ul>
     */
    static Converter<it.pagopa.pn.delivery.models.NotificationSearchRow, InformalNotificationSearchRow> informalSearchRowConverter =
        context -> {
            it.pagopa.pn.delivery.models.NotificationSearchRow source = context.getSource();
            InformalNotificationSearchRow destination = context.getDestination();

            destination.setCommunicationType(
                    org.springframework.util.StringUtils.hasText(source.getCommunicationType())
                            ? InformalNotificationSearchRow.CommunicationTypeEnum.fromValue(source.getCommunicationType())
                            : InformalNotificationSearchRow.CommunicationTypeEnum.INFORMAL);

            if (source.getViewed() != null || source.getDelivered() != null) {
                CommunicationOutcomes outcomes = new CommunicationOutcomes();
                outcomes.setViewed(source.getViewed());
                outcomes.setDelivered(source.getDelivered());
                destination.setCommunicationOutcomes(outcomes);
            }
            return destination;
        };


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
        modelMapper.createTypeMap(NotificationRecipient.class, InformalNotificationRecipientV1.class)
                .addMappings(mapper ->
                        mapper.using(stringToUuid)
                                .map(NotificationRecipient::getMessageId, InformalNotificationRecipientV1::setMessageId)
                );
        modelMapper.createTypeMap(it.pagopa.pn.delivery.models.NotificationSearchRow.class, FullNotificationSearchRow.class)
                .addMappings(mapper -> mapper.skip(FullNotificationSearchRow::setCommunicationType))
                .setPostConverter(ModelMapperConfig.recipientSearchRowConverter);
        modelMapper.createTypeMap(it.pagopa.pn.delivery.models.NotificationSearchRow.class, InformalNotificationSearchRow.class)
                .addMappings(mapper -> mapper.skip(InformalNotificationSearchRow::setCommunicationType))
                .setPostConverter(ModelMapperConfig.informalSearchRowConverter);
        return modelMapper;
    }

}
