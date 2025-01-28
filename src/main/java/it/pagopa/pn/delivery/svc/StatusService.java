package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationDelegationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationCostEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.utils.DataUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
    private final NotificationCostEntityDao notificationCostEntityDao;

    private final PnExternalRegistriesClientImpl externalRegistriesClient;

    public StatusService(NotificationDao notificationDao,
                         NotificationMetadataEntityDao notificationMetadataEntityDao,
                         NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao,
                         NotificationDelegatedService notificationDelegatedService,
                         NotificationCostEntityDao notificationCostEntityDao,
                         PnExternalRegistriesClientImpl externalRegistriesClient) {
        this.notificationDao = notificationDao;
        this.notificationMetadataEntityDao = notificationMetadataEntityDao;
        this.notificationDelegationMetadataEntityDao = notificationDelegationMetadataEntityDao;
        this.notificationDelegatedService = notificationDelegatedService;
        this.notificationCostEntityDao = notificationCostEntityDao;
        this.externalRegistriesClient = externalRegistriesClient;
    }

    public void updateStatus(RequestUpdateStatusDto dto) {
        Optional<InternalNotification> notificationOptional = notificationDao.getNotificationByIun(dto.getIun(), false);

        if(notificationOptional.isEmpty()) {
            throw new PnInternalException("Try to update status for non existing iun=" + dto.getIun(),
                    ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
        }

        InternalNotification notification = notificationOptional.get();
        log.debug("Notification with protocolNumber={} and iun={} is present", notification.getPaProtocolNumber(), dto.getIun());

        NotificationStatusV26 nextStatus = dto.getNextStatus();
        OffsetDateTime acceptedAt;
        switch (nextStatus) {
            case ACCEPTED -> {
                acceptedAt = dto.getTimestamp();
                putNotificationMetadata(dto, notification, acceptedAt);
            }
            case REFUSED ->
                    notification.getRecipients().stream()
                            .filter(r -> Objects.nonNull(r.getPayments()))
                            .forEach(r -> r.getPayments().forEach(notificationPaymentInfo -> {
                                if(notificationPaymentInfo.getPagoPa() != null && StringUtils.hasText(notificationPaymentInfo.getPagoPa().getNoticeCode())){
                                    notificationCostEntityDao.deleteItem(NotificationCostEntity.builder()
                                            .creditorTaxIdNoticeCode(notificationPaymentInfo.getPagoPa().getCreditorTaxId() +"##"+ notificationPaymentInfo.getPagoPa().getNoticeCode())
                                            .build());
                                }
                            }));
            default -> handleDefaultStatusUpdate(dto, notification);
        }
    }

    private void handleDefaultStatusUpdate(RequestUpdateStatusDto dto, InternalNotification notification) {
        OffsetDateTime acceptedAt;
        Key key = Key.builder()
                .partitionValue( notification.getIun() + "##" + notification.getRecipients().get( 0 ).getInternalId() )
                .sortValue( notification.getSentAt().toString() )
                .build();
        Optional<NotificationMetadataEntity> optMetadata = notificationMetadataEntityDao.get( key );
        if (optMetadata.isPresent()) {
            if(!isNewStatus(optMetadata.get(), dto.getTimestamp())) {
                log.debug("Notification with iun={} already has a status with a more recent timestamp", dto.getIun());
                return;
            }

            acceptedAt = OffsetDateTime.parse( optMetadata.get().getTableRow().get( "acceptedAt" ) );
            putNotificationMetadata(dto, notification, acceptedAt);
        } else {
            log.debug( "Unable to retrieve accepted date - iun={} recipientId={}", notification.getIun(), notification.getRecipientIds().get( 0 ) );
        }
    }

    private boolean isNewStatus(NotificationMetadataEntity notificationMetadataEntity, OffsetDateTime timestamp) {
        return notificationMetadataEntity.getNotificationStatusTimestamp() == null ||
                notificationMetadataEntity.getNotificationStatusTimestamp().isBefore( timestamp.toInstant() );
    }

    private void putNotificationMetadata(RequestUpdateStatusDto dto, InternalNotification notification, OffsetDateTime acceptedAt) {
        List<NotificationMetadataEntity> nextMetadataEntry = computeMetadataEntry(dto, notification, acceptedAt);
        nextMetadataEntry.forEach(metadata -> {
            notificationMetadataEntityDao.put(metadata);
            List<NotificationDelegationMetadataEntity> delegationMetadata = notificationDelegatedService.computeDelegationMetadataEntries(metadata);
            delegationMetadata.forEach(notificationDelegationMetadataEntityDao::put);
        });
    }

    private List<NotificationMetadataEntity> computeMetadataEntry(RequestUpdateStatusDto dto, InternalNotification notification, OffsetDateTime acceptedAt) {
        String rootSenderId = externalRegistriesClient.getRootSenderId(notification.getSenderPaId());
        String creationMonth = DataUtils.extractCreationMonth( notification.getSentAt().toInstant() );

        List<String> opaqueTaxIds = notification.getRecipientIds();

        return opaqueTaxIds.stream()
                    .map( recipientId -> this.buildOneSearchMetadataEntry( notification, dto, recipientId, opaqueTaxIds, creationMonth, acceptedAt, rootSenderId))
                    .toList();
    }

    private NotificationMetadataEntity buildOneSearchMetadataEntry(
            InternalNotification notification,
            RequestUpdateStatusDto dto,
            String recipientId,
            List<String> recipientsIds,
            String creationMonth,
            OffsetDateTime acceptedAt,
            String rootSenderId
    ) {
        int recipientIndex = recipientsIds.indexOf( recipientId );
        NotificationStatusV26 lastStatus = dto.getNextStatus();
        OffsetDateTime notificationStatusTimestamp = dto.getTimestamp();

        Map<String,String> tableRowMap = createTableRowMap(notification, recipientsIds, acceptedAt);

        return NotificationMetadataEntity.builder()
                .notificationStatus( lastStatus.toString() )
                .notificationStatusTimestamp( notificationStatusTimestamp.toInstant() )
                .senderId( notification.getSenderPaId() )
                .rootSenderId(rootSenderId)
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
    private Map<String, String> createTableRowMap(InternalNotification notification, List<String> recipientsIds, OffsetDateTime acceptedAt) {
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