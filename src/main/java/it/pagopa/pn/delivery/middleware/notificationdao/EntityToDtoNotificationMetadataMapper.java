package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;


@Component
public class EntityToDtoNotificationMetadataMapper {

    private static final String TABLE_ROW_ACCEPTED_AT = "acceptedAt";
    private static final String TABLE_ROW_SENDER_DENOMINATION = "senderDenomination";
    private static final String TABLE_ROW_SUBJECT = "subject";
    private static final String TABLE_ROW_PA_PROTOCOL_NUMBER = "paProtocolNumber";

    public NotificationSearchRow entity2Dto(NotificationMetadataEntity entity) {
        OffsetDateTime requestAcceptedAt = null;

        final Map<String, String> tableRow = entity.getTableRow();
        if ( (tableRow.get( TABLE_ROW_ACCEPTED_AT ) != null) ) {
            requestAcceptedAt = OffsetDateTime.parse( tableRow.get( TABLE_ROW_ACCEPTED_AT ) );
        }
        
        return NotificationSearchRow.builder()
                .iun( entity.getIunRecipientId().substring(0 ,entity.getIunRecipientId().indexOf("##")) )
                .sender( tableRow.get( TABLE_ROW_SENDER_DENOMINATION ) )
                .recipients( entity.getRecipientIds() )
                .sentAt( entity.getSentAt().atOffset( ZoneOffset.UTC ))
                .subject( tableRow.get( TABLE_ROW_SUBJECT ) )
                .paProtocolNumber( tableRow.get(TABLE_ROW_PA_PROTOCOL_NUMBER) )
                .requestAcceptedAt( requestAcceptedAt )
                .group( entity.getNotificationGroup() )
                .notificationStatus( NotificationStatus.valueOf( entity.getNotificationStatus() ))
                .build();
    }

    public NotificationSearchRow entity2Dto(NotificationDelegationMetadataEntity entity) {
        OffsetDateTime requestAcceptedAt = null;

        final Map<String, String> tableRow = entity.getTableRow();
        if (tableRow.get(TABLE_ROW_ACCEPTED_AT) != null) {
            requestAcceptedAt = OffsetDateTime.parse(tableRow.get(TABLE_ROW_ACCEPTED_AT));
        }

        return NotificationSearchRow.builder()
                .iun(entity.getIunRecipientIdDelegateIdGroupId().substring(0, entity.getIunRecipientIdDelegateIdGroupId().indexOf("##")))
                .sender(tableRow.get(TABLE_ROW_SENDER_DENOMINATION))
                .recipients(Collections.singletonList(entity.getRecipientId()))
                .sentAt(entity.getSentAt().atOffset(ZoneOffset.UTC))
                .subject(tableRow.get(TABLE_ROW_SUBJECT))
                .paProtocolNumber(tableRow.get(TABLE_ROW_PA_PROTOCOL_NUMBER))
                .requestAcceptedAt(requestAcceptedAt)
                .notificationStatus(NotificationStatus.valueOf(entity.getNotificationStatus()))
                .mandateId(entity.getMandateId())
                .build();
    }
}
