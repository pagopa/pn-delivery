package it.pagopa.pn.delivery.middleware.notificationdao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.pagopa.pn.api.dto.notification.*;
import it.pagopa.pn.delivery.model.notification.cassandra.NotificationEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//@Mapper( componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Component
public class EntityToDtoMapper {

    private final ObjectMapper objMapper;
    private final ObjectReader recipientReader;

    public EntityToDtoMapper(ObjectMapper objMapper) {
        this.objMapper = objMapper;
        this.recipientReader = this.objMapper.readerFor( NotificationRecipient.class );
    }

    public Notification entity2Dto(NotificationEntity entity) {
        Notification.NotificationBuilder builder = Notification.builder()
                .iun( entity.getIun() )
                .subject( entity.getSubject() )
                .paNotificationId( entity.getPaNotificationId() )
                .cancelledByIun( entity.getCancelledByIun() )
                .cancelledIun( entity.getCancelledIun() )

                .sender( NotificationSender.builder()
                        .paId( entity.getSenderPaId() )
                        .build()
                    )
                .recipients( buildRecipientsList( entity ) )

                .documents( buildDocumentsList( entity ) );

        boolean anyPaymentFieldNotNull = Arrays.asList(
                    entity.getIuv(), entity.getNotificationFeePolicy(),
                    entity.getF24AnalogDigestSha256(), entity.getF24AnalogVersionId(),
                entity.getF24DigitalDigestSha256(), entity.getF24DigitalVersionId(),
                entity.getF24FlatRateDigestSha256(), entity.getF24FlatRateVersionId()
                )
                .stream().anyMatch( val -> val != null);
        if ( anyPaymentFieldNotNull ) {
            builder.payment( buildNotificationPaymentInfo( entity ) );
        }

        // Generated from timeline table
        //.notificationStatus()
        //.notificationStatusHistory()
        //.timeline()

        return builder.build();
    }

    private NotificationPaymentInfo buildNotificationPaymentInfo(NotificationEntity entity) {

        NotificationPaymentInfo.NotificationPaymentInfoBuilder builder =  NotificationPaymentInfo.builder()
                .iuv( entity.getIuv() )
                .notificationFeePolicy( entity.getNotificationFeePolicy() )
                ;

        NotificationAttachment f24Digital = buildF24Attachment(
                             entity.getF24DigitalVersionId(), entity.getF24DigitalDigestSha256() );
        NotificationAttachment f24Analog = buildF24Attachment(
                             entity.getF24AnalogVersionId(), entity.getF24AnalogDigestSha256() );
        NotificationAttachment f24FlatRate = buildF24Attachment(
                           entity.getF24FlatRateVersionId(), entity.getF24FlatRateDigestSha256() );

        boolean anyF24moduleNotNull = Arrays.asList( f24Analog, f24Digital, f24FlatRate )
                .stream().anyMatch( f24 -> f24 != null);

        if( anyF24moduleNotNull ) {
            builder.f24( NotificationPaymentInfo.F24.builder()
                    .analog( f24Analog )
                    .digital( f24Digital )
                    .flatRate( f24FlatRate )
                    .build()
            );
        }

        return builder.build();
    }

    private NotificationAttachment buildF24Attachment( String version, String sha256 ) {
        NotificationAttachment result;
        if ( StringUtils.isAllBlank( version, sha256 ) ) {
            result = null;
        }
        else if ( StringUtils.isNotBlank( version ) && StringUtils.isNotBlank( sha256 ) ) {
            result = NotificationAttachment.builder()
                    .savedVersionId( version )
                    .digests( NotificationAttachment.Digests.builder()
                            .sha256( sha256 )
                            .build()
                    )
                    .build();
        }
        else {
            // FIXME messaggistica ed eccezioni
            throw new IllegalStateException( "Error version (" + version + ") and sha256 (" + sha256 + ") are both required or both blank" );
        }
        return result;
    }

    private List<NotificationAttachment> buildDocumentsList( NotificationEntity entity ) {
        List<String> documentsDigestsSha256 = entity.getDocumentsDigestsSha256();
        List<String> documentsVersionIds = entity.getDocumentsVersionIds();

        int length = documentsDigestsSha256.size();
        if ( length != documentsVersionIds.size() ) {
            throw new IllegalStateException(" Notification entity with iun " + entity.getIun() + " hash different quantity of document versions and sha"); // FIXME handle exceptions
        }

        List<NotificationAttachment> result = new ArrayList<>();
        for( int d = 0; d < length; d += 1 ) {
            NotificationAttachment notificationAttachment = NotificationAttachment.builder()
                    .savedVersionId( documentsVersionIds.get( d ) )
                    .digests( NotificationAttachment.Digests.builder()
                            .sha256( documentsDigestsSha256.get( d ) )
                            .build())
                    .build();
            result.add( notificationAttachment );
        }

        return result;
    }

    private List<NotificationRecipient> buildRecipientsList( NotificationEntity entity ) {
        Map<String, String> recipientsMetadata = entity.getRecipientsJson();

        return entity.getRecipientsOrder().stream()
                .map( recipientId -> parseRecipientJson( recipientsMetadata.get( recipientId)) )
                .collect(Collectors.toList());

    }

    private NotificationRecipient parseRecipientJson( String jsonString ) {
        try {
            return this.recipientReader.readValue( jsonString );
        } catch (JsonProcessingException exc) {
            throw new IllegalStateException( exc ); // FIXME gestione eccezioni
        }
    }
}
