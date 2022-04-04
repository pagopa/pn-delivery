package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoNotificationMetadataMapper {

    public NotificationSearchRow entity2Dto(NotificationMetadataEntity entity) {
        return NotificationSearchRow.builder()
                .iun( entity.getIun_recipientId().substring(0 ,entity.getIun_recipientId().indexOf("##")) )
                .senderId( entity.getSenderId() )
                .recipientId( entity.getRecipientId() )
                .sentAt( entity.getSentAt() )
                .subject( entity.getTableRow().get( "subject" ) )
                //.paNotificationId(  ) //TODO non presente in NotificationMetadataEntity aggiungerla nel TableRow ??
                .notificationStatus( NotificationStatus.valueOf( entity.getNotificationStatus() ))
                .build();
    }
}
