package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineInfoDto;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatusService {
    private final NotificationDao notificationDao;
    private final StatusUtils statusUtils;
    private final NotificationMetadataEntityDao<Key, NotificationMetadataEntity> notificationMetadataEntityDao;


    public StatusService(NotificationDao notificationDao, StatusUtils statusUtils,
            NotificationMetadataEntityDao notificationMetadataEntityDao) {
        this.notificationDao = notificationDao;
        this.statusUtils = statusUtils;
        this.notificationMetadataEntityDao = notificationMetadataEntityDao;
    }


    public ResponseUpdateStatusDto updateStatus(RequestUpdateStatusDto dto) {
        Optional<Notification> notificationOptional = notificationDao.getNotificationByIun(dto.getIun());
        
        ResponseUpdateStatusDto responseDto;
        
        if (notificationOptional.isPresent()) {
            Notification notification = notificationOptional.get();
            log.debug("Notification is present {} for iun {}", notification.getPaNotificationId(), dto.getIun());

            Set<TimelineInfoDto> currentTimeline = dto.getCurrentTimeline();

            // - Calcolare lo stato corrente
            NotificationStatus currentState =  computeLastStatusHistoryElement( notification, currentTimeline ).getStatus();
            log.debug("CurrentState is {} for iun {}",currentState, dto.getIun());
            
            currentTimeline.add(dto.getNewTimelineElement());
            
            // - Calcolare il nuovo stato
            NotificationStatusHistoryElement nextState = computeLastStatusHistoryElement( notification, currentTimeline );
            List<NotificationMetadataEntity> nextMetadataEntry = computeMetadataEntry(nextState, notification, currentTimeline);

            log.debug("Next state is {} for iun {}",nextState.getStatus(), dto.getIun());

            // - se i due stati differiscono
            if (!currentState.equals(nextState.getStatus()) && !nextState.getStatus().equals( NotificationStatus.REFUSED )) {
                log.info("Change status from {} to {} for iun {}",currentState, nextState.getStatus(), dto.getIun());
                addNotificationMetadataEntries( nextMetadataEntry );
            }

            responseDto = ResponseUpdateStatusDto.builder()
                    .currentStatus(currentState)
                    .nextStatus(nextState.getStatus())
                    .build();
        } else {
            throw new PnInternalException("Try to update status for non existing iun " + dto.getIun());
        }
        
        return responseDto;
    }

    private void addNotificationMetadataEntries(List<NotificationMetadataEntity> nextMetadataEntry) {
        nextMetadataEntry.forEach( notificationMetadataEntityDao::put );
    }

    private NotificationStatusHistoryElement computeLastStatusHistoryElement(Notification notification, Set<TimelineInfoDto> currentTimeline) {
        int numberOfRecipient = notification.getRecipients().size();
        Instant notificationCreatedAt = notification.getSentAt();

        List<NotificationStatusHistoryElement> historyElementList = statusUtils.getStatusHistory(
                currentTimeline,
                numberOfRecipient,
                notificationCreatedAt);

        return historyElementList.get(historyElementList.size() - 1);
    }


    private List<NotificationMetadataEntity> computeMetadataEntry(NotificationStatusHistoryElement lastStatus, Notification notification, Set<TimelineInfoDto> currentTimeline) {
        int numberOfRecipient = notification.getRecipients().size();
        Instant notificationCreatedAt = notification.getSentAt();
        String creationMonth = extractCreationMonth( notificationCreatedAt );

        List<NotificationMetadataEntity> resultList = new ArrayList<>();

        List<String> recipientIds = notification.getRecipients().stream()
                .map( NotificationRecipient::getTaxId )
                .collect(Collectors.toList() );

        notification.getRecipients().forEach( recipient -> resultList.add( NotificationMetadataEntity.builder()
                .notificationStatus( NotificationStatus.valueOf(lastStatus.getStatus().toString() ).toString() )
                .senderId( notification.getSender().getPaId() )
                .recipientId( recipient.getTaxId() )
                .sentAt( notificationCreatedAt )
                //.notificationGroup( notification.getGroup() )
                .recipientIds( recipientIds )
                .tableRow( Map.ofEntries(
                        Map.entry( "iun", notification.getIun() ),
                        Map.entry( "recipientsIds", recipientIds.toString() ),
                        Map.entry( "paNotificationId", notification.getPaNotificationId() ),
                        Map.entry( "subject", notification.getSubject())  ) )
                .senderId_recipientId( createConcatenation( notification.getSender().getPaId(), recipient.getTaxId()  ) )
                .senderId_creationMonth( createConcatenation( notification.getSender().getPaId(), creationMonth ) )
                .recipientId_creationMonth( createConcatenation( recipient.getTaxId() , creationMonth ) )
                .iun_recipientId( createConcatenation( notification.getIun(), recipient.getTaxId() ) )
                .recipientOne( numberOfRecipient <= 1 )
                .build() ) );

        return resultList;
    }

    private String createConcatenation(String s1, String s2) {
        return s1 + "##" + s2;
    }

    private String extractCreationMonth(Instant sentAt) {
        String sentAtString = sentAt.toString();
        String[] splitSentAt = sentAtString.split( "-" );
        return splitSentAt[0] + splitSentAt[1];
    }

}
