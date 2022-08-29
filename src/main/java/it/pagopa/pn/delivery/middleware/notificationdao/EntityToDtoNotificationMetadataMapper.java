package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;


@Component
public class EntityToDtoNotificationMetadataMapper {

    public NotificationSearchRow entity2Dto(NotificationMetadataEntity entity) {
        OffsetDateTime requestAcceptedAt = null;

        final Map<String, String> tableRow = entity.getTableRow();
        if ((tableRow != null) && (tableRow.get( "acceptedAt" ) != null)) {
            requestAcceptedAt = OffsetDateTime.parse( tableRow.get( "acceptedAt" ) );
        }
        
        return NotificationSearchRow.builder()
                .iun( entity.getIun_recipientId().substring(0 ,entity.getIun_recipientId().indexOf("##")) )
                .sender( tableRow.get( "senderDenomination" ) )
                .recipients( entity.getRecipientIds() )
                .sentAt( entity.getSentAt().atOffset( ZoneOffset.UTC ))
                .subject( tableRow.get( "subject" ) )
                .paProtocolNumber( tableRow.get("paProtocolNumber") )
                .requestAcceptedAt( requestAcceptedAt )
                .group( entity.getNotificationGroup() )
                .notificationStatus( NotificationStatus.valueOf( entity.getNotificationStatus() ))
                .build();
    }

}
