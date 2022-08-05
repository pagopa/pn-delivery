package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatusService {
    private final NotificationDao notificationDao;
    private final NotificationMetadataEntityDao notificationMetadataEntityDao;
    private final PnDataVaultClientImpl dataVaultClient;

    public StatusService(NotificationDao notificationDao,
                         NotificationMetadataEntityDao notificationMetadataEntityDao, PnDataVaultClientImpl dataVaultClient) {
        this.notificationDao = notificationDao;
        this.notificationMetadataEntityDao = notificationMetadataEntityDao;
        this.dataVaultClient = dataVaultClient;
    }
    
    public void updateStatus(RequestUpdateStatusDto dto) {
        Optional<InternalNotification> notificationOptional = notificationDao.getNotificationByIun(dto.getIun());
        
        if (notificationOptional.isPresent()) {
            InternalNotification notification = notificationOptional.get();
            log.debug("Notification with protocolNumber={} and iun={} is present", notification.getPaProtocolNumber(), dto.getIun());

            List<NotificationMetadataEntity> nextMetadataEntry = computeMetadataEntry(dto, notification);
            nextMetadataEntry.forEach( notificationMetadataEntityDao::put );
        } else {
            throw new PnInternalException("Try to update status for non existing iun=" + dto.getIun());
        }
    }

    private List<NotificationMetadataEntity> computeMetadataEntry(RequestUpdateStatusDto dto, InternalNotification notification) {
        NotificationStatus lastStatus = dto.getNextStatus();
        OffsetDateTime timestamp = dto.getTimestamp();
        String creationMonth = extractCreationMonth( notification.getSentAt().toInstant() );

        List<String> opaqueTaxIds = new ArrayList<>();
        for (NotificationRecipient recipient : notification.getRecipients()) {
            opaqueTaxIds.add( dataVaultClient.ensureRecipientByExternalId( RecipientType.fromValue(recipient.getRecipientType().getValue()), recipient.getTaxId() ));
        }

        return opaqueTaxIds.stream()
                    .map( recipientId -> this.buildOneSearchMetadataEntry( notification, lastStatus, recipientId, opaqueTaxIds, creationMonth, timestamp))
                    .collect(Collectors.toList());
    }

    private NotificationMetadataEntity buildOneSearchMetadataEntry(
            InternalNotification notification,
            NotificationStatus lastStatus,
            String recipientId,
            List<String> recipientsIds,
            String creationMonth,
            OffsetDateTime timestamp
    ) {
        int recipientIndex = recipientsIds.indexOf( recipientId );

        Map<String,String> tableRowMap = createTableRowMap(notification, lastStatus, recipientsIds, timestamp);

        return NotificationMetadataEntity.builder()
                .notificationStatus( lastStatus.toString() )
                .senderId( notification.getSenderPaId() )
                .recipientId( recipientId )
                .sentAt( notification.getSentAt().toInstant() )
                .notificationGroup( notification.getGroup() )
                .recipientIds( recipientsIds )
                .tableRow( tableRowMap )
                .senderId_recipientId( createConcatenation( notification.getSenderPaId(), recipientId  ) )
                .senderId_creationMonth( createConcatenation( notification.getSenderPaId(), creationMonth ) )
                .recipientId_creationMonth( createConcatenation( recipientId , creationMonth ) )
                .iun_recipientId( createConcatenation( notification.getIun(), recipientId ) )
                .recipientOne( recipientIndex <= 0 )
                .build();
    }

    @NotNull
    private Map<String, String> createTableRowMap(InternalNotification notification, NotificationStatus lastStatus, List<String> recipientsIds, OffsetDateTime timestamp) {
        Map<String,String> tableRowMap = new HashMap<>();
        tableRowMap.put( "iun", notification.getIun() );
        tableRowMap.put( "recipientsIds", recipientsIds.toString() );
        tableRowMap.put( "paProtocolNumber", notification.getPaProtocolNumber() );
        tableRowMap.put( "subject", notification.getSubject() );
        tableRowMap.put( "senderDenomination", notification.getSenderDenomination() );
        if ( NotificationStatus.ACCEPTED.equals(lastStatus)) {
            tableRowMap.put( "acceptedAt", timestamp.toString() );
        }
        return tableRowMap;
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
