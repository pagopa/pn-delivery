package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.delivery.middleware.model.notification.NotificationMetadataEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoNotificationMetadataMapper {

    public NotificationSearchRow entity2Dto(NotificationMetadataEntity entity) {
        return NotificationSearchRow.builder()
                .iun( entity.getIun_recipientId().substring(0 ,entity.getIun_recipientId().indexOf("##")) )
                .senderId( entity.getSenderId() )
                .recipientIds( entity.getRecipientIds() )
                .sentAt( entity.getSentAt() )
                .notificationStatus( NotificationStatus.valueOf( entity.getNotificationStatus() ))
                .build();
    }
}
