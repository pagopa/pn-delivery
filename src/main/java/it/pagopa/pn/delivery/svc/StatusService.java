package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationDelegationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.utils.DataUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.OffsetDateTime;
import java.util.*;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND;

@Slf4j
@Service
public class StatusService {

    private final NotificationDao notificationDao;
    private final NotificationMetadataEntityDao notificationMetadataEntityDao;
    private final NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao;
    private final NotificationDelegatedService notificationDelegatedService;
    private final PnDataVaultClientImpl dataVaultClient;

    public StatusService(NotificationDao notificationDao,
                         NotificationMetadataEntityDao notificationMetadataEntityDao,
                         NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao,
                         NotificationDelegatedService notificationDelegatedService,
                         PnDataVaultClientImpl dataVaultClient) {
        this.notificationDao = notificationDao;
        this.notificationMetadataEntityDao = notificationMetadataEntityDao;
        this.notificationDelegationMetadataEntityDao = notificationDelegationMetadataEntityDao;
        this.notificationDelegatedService = notificationDelegatedService;
        this.dataVaultClient = dataVaultClient;
    }
    
    public void updateStatus(RequestUpdateStatusDto dto) {
        Optional<InternalNotification> notificationOptional = notificationDao.getNotificationByIun(dto.getIun());
        
        if (notificationOptional.isPresent()) {
            InternalNotification notification = notificationOptional.get();
            log.debug("Notification with protocolNumber={} and iun={} is present", notification.getPaProtocolNumber(), dto.getIun());

            OffsetDateTime acceptedAt = null;

            if ( !NotificationStatus.ACCEPTED.equals( dto.getNextStatus() ) ) {
                Key key = Key.builder()
                        .partitionValue( notification.getIun() + "##" + notification.getRecipients().get( 0 ).getInternalId() )
                        .sortValue( notification.getSentAt().toString() )
                        .build();
                Optional<NotificationMetadataEntity> optMetadata = notificationMetadataEntityDao.get( key );
                if (optMetadata.isPresent() ) {
                    acceptedAt = OffsetDateTime.parse( optMetadata.get().getTableRow().get( "acceptedAt" ) );
                } else {
                    log.debug( "Unable to retrieve accepted date - iun={} recipientId={}", notification.getIun(), notification.getRecipientIds().get( 0 ) );
                }
            } else {
                acceptedAt = dto.getTimestamp();
            }

            List<NotificationMetadataEntity> nextMetadataEntry = computeMetadataEntry(dto, notification, acceptedAt);
            nextMetadataEntry.forEach(metadata -> {
                notificationMetadataEntityDao.put(metadata);
                List<NotificationDelegationMetadataEntity> delegationMetadata = notificationDelegatedService.computeDelegationMetadataEntries(metadata);
                delegationMetadata.forEach(notificationDelegationMetadataEntityDao::put);
            });
        } else {
            throw new PnInternalException("Try to update status for non existing iun=" + dto.getIun(),
                    ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
        }
    }

    private List<NotificationMetadataEntity> computeMetadataEntry(RequestUpdateStatusDto dto, InternalNotification notification, OffsetDateTime acceptedAt) {
        NotificationStatus lastStatus = dto.getNextStatus();
        String creationMonth = DataUtils.extractCreationMonth( notification.getSentAt().toInstant() );

        List<String> opaqueTaxIds = new ArrayList<>();
        for (NotificationRecipient recipient : notification.getRecipients()) {
            opaqueTaxIds.add( dataVaultClient.ensureRecipientByExternalId( RecipientType.fromValue(recipient.getRecipientType().getValue()), recipient.getTaxId() ));
        }

        return opaqueTaxIds.stream()
                    .map( recipientId -> this.buildOneSearchMetadataEntry( notification, lastStatus, recipientId, opaqueTaxIds, creationMonth, acceptedAt))
                    .toList();
    }

    private NotificationMetadataEntity buildOneSearchMetadataEntry(
            InternalNotification notification,
            NotificationStatus lastStatus,
            String recipientId,
            List<String> recipientsIds,
            String creationMonth,
            OffsetDateTime acceptedAt
    ) {
        int recipientIndex = recipientsIds.indexOf( recipientId );

        Map<String,String> tableRowMap = createTableRowMap(notification, lastStatus, recipientsIds, acceptedAt);

        return NotificationMetadataEntity.builder()
                .notificationStatus( lastStatus.toString() )
                .senderId( notification.getSenderPaId() )
                .recipientId( recipientId )
                .sentAt( notification.getSentAt().toInstant() )
                .notificationGroup( notification.getGroup() )
                .recipientIds( recipientsIds )
                .tableRow( tableRowMap )
                .senderIdRecipientId( DataUtils.createConcatenation( notification.getSenderPaId(), recipientId  ) )
                .senderIdCreationMonth( DataUtils.createConcatenation( notification.getSenderPaId(), creationMonth ) )
                .recipientIdCreationMonth( DataUtils.createConcatenation( recipientId , creationMonth ) )
                .iunRecipientId( DataUtils.createConcatenation( notification.getIun(), recipientId ) )
                .recipientOne( recipientIndex <= 0 )
                .build();
    }

    @NotNull
    private Map<String, String> createTableRowMap(InternalNotification notification, NotificationStatus lastStatus, List<String> recipientsIds, OffsetDateTime acceptedAt) {
        Map<String,String> tableRowMap = new HashMap<>();
        tableRowMap.put( "iun", notification.getIun() );
        tableRowMap.put( "recipientsIds", recipientsIds.toString() );
        tableRowMap.put( "paProtocolNumber", notification.getPaProtocolNumber() );
        tableRowMap.put( "subject", notification.getSubject() );
        tableRowMap.put( "senderDenomination", notification.getSenderDenomination() );
        if ( Objects.nonNull( acceptedAt )) {
            tableRowMap.put( "acceptedAt", acceptedAt.toString() );
        }
        return tableRowMap;
    }

}
