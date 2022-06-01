package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import org.springframework.stereotype.Component;

import java.util.Date;


@Component
public class EntityToDtoNotificationMetadataMapper {

    public NotificationSearchRow entity2Dto(NotificationMetadataEntity entity) {
        return NotificationSearchRow.builder()
                .iun( entity.getIun_recipientId().substring(0 ,entity.getIun_recipientId().indexOf("##")) )
                .sender( entity.getSenderId() )
                .recipients( entity.getRecipientIds() )
                .sentAt( Date.from(entity.getSentAt() ))
                .subject( entity.getTableRow().get( "subject" ) )
                .paProtocolNumber( entity.getTableRow().get("paProtocolNumber") )
                //.group( entity.getNotificationGroup() )
                .notificationStatus( NotificationStatus.valueOf( entity.getNotificationStatus() ))
                .build();
    }

}
