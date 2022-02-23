package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.notificationdao.CassandraNotificationByRecipientEntityDao;
import it.pagopa.pn.commons_delivery.middleware.notificationdao.CassandraNotificationBySenderEntityDao;
import it.pagopa.pn.commons_delivery.middleware.notificationdao.CassandraNotificationEntityDao;
import it.pagopa.pn.commons_delivery.model.notification.cassandra.*;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class StatusService {
    private final CassandraNotificationEntityDao notificationEntityDao;
    private final StatusUtils statusUtils;
    private final CassandraNotificationBySenderEntityDao notificationBySenderEntityDao;
    private final CassandraNotificationByRecipientEntityDao notificationByRecipientEntityDao;

    public StatusService(CassandraNotificationEntityDao notificationEntityDao, StatusUtils statusUtils,
                         CassandraNotificationBySenderEntityDao notificationBySenderEntityDao, CassandraNotificationByRecipientEntityDao notificationByRecipientEntityDao) {
        this.notificationEntityDao = notificationEntityDao;
        this.statusUtils = statusUtils;
        this.notificationBySenderEntityDao = notificationBySenderEntityDao;
        this.notificationByRecipientEntityDao = notificationByRecipientEntityDao;
    }


    public ResponseUpdateStatusDto updateStatus(RequestUpdateStatusDto dto) {
        Optional<NotificationEntity> notificationEntityOptional = notificationEntityDao.get(dto.getIun());
        
        ResponseUpdateStatusDto responseDto;
        
        if (notificationEntityOptional.isPresent()) {
            NotificationEntity notificationEntity = notificationEntityOptional.get();
            log.debug("Notification entity is present {} for iun {}", notificationEntity.getPaNotificationId(), dto.getIun());

            Set<TimelineElement> currentTimeline = dto.getCurrentTimeline();

            // - Calcolare lo stato corrente
            NotificationBySenderEntity currentSearchBySenderEntry = computeSearchBySenderEntry(notificationEntity, currentTimeline);
            NotificationStatus currentState = currentSearchBySenderEntry.getNotificationBySenderId().getNotificationStatus();
            NotificationByRecipientEntity currentSearchByRecipientEntry = computeSearchByRecipientEntry(notificationEntity, currentTimeline);
            log.debug("CurrentState is {} for iun {}",currentState, dto.getIun());
            
            currentTimeline.add(dto.getNewTimelineElement());
            
            // - Calcolare il nuovo stato
            NotificationBySenderEntity nextSearchBySenderEntry = computeSearchBySenderEntry(notificationEntity, currentTimeline);
            NotificationStatus nextState = nextSearchBySenderEntry.getNotificationBySenderId().getNotificationStatus();
            NotificationByRecipientEntity nextSearchByRecipientEntry = computeSearchByRecipientEntry(notificationEntity, currentTimeline);

            log.debug("Next state is {} for iun {}",nextState, dto.getIun());

            // - se i due stati differiscono
            if (!currentState.equals(nextState)) {
                log.info("Change status from {} to {} for iun {}",currentState, nextState, dto.getIun());
                addNewSearchEntries(nextSearchBySenderEntry, nextSearchByRecipientEntry, notificationEntity);
                deleteOldSearchEntries(currentSearchBySenderEntry, currentSearchByRecipientEntry, notificationEntity);
            }

            responseDto = ResponseUpdateStatusDto.builder()
                    .currentStatus(currentState)
                    .nextStatus(nextState)
                    .build();
        } else {
            throw new PnInternalException("Try to update status for non existing iun " + dto.getIun());
        }
        
        return responseDto;
    }


    private void deleteOldSearchEntries(NotificationBySenderEntity nextSearchBySenderEntry, NotificationByRecipientEntity nextSearchByRecipientEntry, NotificationEntity notificationEntity) {

        for (String recipientId : notificationEntity.getRecipientsOrder()) {
            notificationBySenderEntityDao.delete(
                    nextSearchBySenderEntry.getNotificationBySenderId().toBuilder()
                            .recipientId(recipientId)
                            .build()
            );
            notificationByRecipientEntityDao.delete(
                    nextSearchByRecipientEntry.getNotificationByRecipientId().toBuilder()
                            .recipientId(recipientId)
                            .build()
            );
        }

    }

    private void addNewSearchEntries(NotificationBySenderEntity currentSearchBySenderEntry, NotificationByRecipientEntity currentSearchByRecipientEntry, NotificationEntity notificationEntity) {

        for (String recipientId : notificationEntity.getRecipientsOrder()) {
            notificationBySenderEntityDao.put(currentSearchBySenderEntry.toBuilder()
                    .notificationBySenderId(currentSearchBySenderEntry.getNotificationBySenderId()
                            .toBuilder()
                            .recipientId(recipientId)
                            .build()
                    )
                    .build());
            notificationByRecipientEntityDao.put(currentSearchByRecipientEntry.toBuilder()
                    .notificationByRecipientId(currentSearchByRecipientEntry.getNotificationByRecipientId()
                            .toBuilder()
                            .recipientId(recipientId)
                            .build()
                    )
                    .build());
        }

    }

    private NotificationBySenderEntity computeSearchBySenderEntry(NotificationEntity notificationEntity, Set<TimelineElement> currentTimeline) {
        int numberOfRecipient = notificationEntity.getRecipientsOrder().size();
        Instant notificationCreatedAt = notificationEntity.getSentAt();

        List<NotificationStatusHistoryElement> historyElementList = statusUtils.getStatusHistory(
                currentTimeline,
                numberOfRecipient,
                notificationCreatedAt);

        NotificationStatusHistoryElement lastStatus = historyElementList.get(historyElementList.size() - 1);

        return NotificationBySenderEntity.builder()
                .notificationBySenderId(NotificationBySenderEntityId.builder()
                        .notificationStatus(lastStatus.getStatus())
                        .senderId(notificationEntity.getSenderPaId())
                        .sentat(notificationCreatedAt)
                        .recipientId(null)
                        .iun(notificationEntity.getIun())
                        .build()
                )
                .paNotificationId(notificationEntity.getPaNotificationId())
                .recipientsJson(notificationEntity.getRecipientsJson())
                .subject(notificationEntity.getSubject())
                .build();
    }

    private NotificationByRecipientEntity computeSearchByRecipientEntry(NotificationEntity notificationEntity, Set<TimelineElement> currentTimeline) {
        int numberOfRecipient = notificationEntity.getRecipientsOrder().size();
        Instant notificationCreatedAt = notificationEntity.getSentAt();

        List<NotificationStatusHistoryElement> historyElementList = statusUtils.getStatusHistory(
                currentTimeline,
                numberOfRecipient,
                notificationCreatedAt);

        NotificationStatusHistoryElement lastStatus;
        lastStatus = historyElementList.get(historyElementList.size() - 1);

        return NotificationByRecipientEntity.builder()
                .paNotificationId(notificationEntity.getPaNotificationId())
                .recipientsJson(notificationEntity.getRecipientsJson())
                .subject(notificationEntity.getSubject())
                .notificationByRecipientId(
                        NotificationByRecipientEntityId.builder()
                                .notificationStatus(lastStatus.getStatus())
                                .senderId(notificationEntity.getSenderPaId())
                                .sentat(notificationCreatedAt)
                                .iun(notificationEntity.getIun())
                                .build()
                )
                .build();
    }

}
