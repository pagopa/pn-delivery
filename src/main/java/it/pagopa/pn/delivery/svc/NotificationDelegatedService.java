package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.PnMandateEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.DelegateType;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationDelegationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.svc.search.IndexNameAndPartitions;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import it.pagopa.pn.delivery.utils.DataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.*;

@Slf4j
@Service
public class NotificationDelegatedService {
    private static final int QUERY_RESULTS_SIZE = 1000;

    private final NotificationMetadataEntityDao notificationMetadataEntityDao;
    private final NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao;
    private final PnMandateClientImpl mandateClient;

    private static final int DEFAULT_DYNAMO_QUERY_SIZE = 1000;

    public NotificationDelegatedService(NotificationMetadataEntityDao notificationMetadataEntityDao,
                                        NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao,
                                        PnMandateClientImpl mandateClient) {
        this.notificationMetadataEntityDao = notificationMetadataEntityDao;
        this.notificationDelegationMetadataEntityDao = notificationDelegationMetadataEntityDao;
        this.mandateClient = mandateClient;
    }

    public void handleAcceptedMandate(PnMandateEvent.Payload event) {
        log.info("handling accepted mandate: {}, delegator={}, delegate={}", event.getMandateId(), event.getDelegatorId(), event.getDelegateId());
        InternalMandateDto mandate = getMandateByDelegator(event.getDelegatorId(), event.getMandateId())
                .orElseThrow(() -> new PnInternalException("Mandate not found", ERROR_CODE_DELIVERY_MANDATENOTFOUND));
        log.debug("mandate: {}", mandate);
        List<InternalMandateDto> listOfOneMandate = List.of(mandate);

        InputSearchNotificationDto searchDto = InputSearchNotificationDto.builder()
                .senderReceiverId(event.getDelegatorId())
                .bySender(false)
                .startDate(event.getValidFrom())
                .endDate(Instant.now()) // duplico solo le notifiche ricevute fino ad adesso, quelle successive verranno duplicate tramite il flusso standard
                .size(DEFAULT_DYNAMO_QUERY_SIZE)
                .build();
        log.debug("filters: {}", searchDto);

        IndexNameAndPartitions indexAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(searchDto);
        if (IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_RECEIVER != indexAndPartitions.getIndexName()) {
            log.error("index must be on receiverId instead of {}", indexAndPartitions.getIndexName());
            throw new PnInternalException("Index not valid", ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME);
        }
        log.debug("accepted mandate indexAndPartitions: {}", indexAndPartitions);

        int globalIterations = 0;
        int duplicationCounter = 0;
        for (String partition : indexAndPartitions.getPartitions()) {
            PnLastEvaluatedKey lastEvaluatedKey = null;
            int partitionIterations = 0;
            do {
                PageSearchTrunk<NotificationMetadataEntity> page = notificationMetadataEntityDao.searchForOneMonth(searchDto,
                        IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_RECEIVER.getValue(),
                        partition,
                        DEFAULT_DYNAMO_QUERY_SIZE,
                        lastEvaluatedKey);
                List<NotificationDelegationMetadataEntity> entries = page.getResults().stream()
                        .flatMap(metadata -> computeDelegationMetadataEntries(metadata, listOfOneMandate))
                        .toList();
                log.debug("saving {} delegation metadata entries", entries.size());
                List<NotificationDelegationMetadataEntity> unprocessed = notificationDelegationMetadataEntityDao.batchPutItems(entries);
                if (!unprocessed.isEmpty()) {
                    log.error("can not save all delegation metadata entities - unprocessed entries = {}", unprocessed.size());
                    throw new PnInternalException("Can not save all delegation metadata entities", ERROR_CODE_DELIVERY_HANDLEEVENTFAILED);
                }
                duplicationCounter += entries.size();
                lastEvaluatedKey = lastEvaluatedKeyFromPage(page, partition);
                log.debug("last evaluated key: {}", lastEvaluatedKey);
                partitionIterations++;
            } while (lastEvaluatedKey != null);
            globalIterations += partitionIterations;
            log.info("mandateId={} partition={} iterations={}", event.getMandateId(), partition, partitionIterations);
        }
        log.info("mandateId={} duplicationCounter={} globalIterations={}", event.getMandateId(), duplicationCounter, globalIterations);
    }

    public void deleteNotificationDelegatedByMandateId(String mandateId, String eventType) {
        log.info("handling {} mandate: {}", eventType, mandateId);

        PnLastEvaluatedKey startEvaluatedKey = new PnLastEvaluatedKey();
        PageSearchTrunk<NotificationDelegationMetadataEntity> oneQueryResult;

        do {
            log.info("querying delegated notification by mandateId: {}, {} results at a time", mandateId, QUERY_RESULTS_SIZE);
            oneQueryResult = notificationDelegationMetadataEntityDao.searchDelegatedByMandateId(mandateId,
                    DEFAULT_DYNAMO_QUERY_SIZE,
                    startEvaluatedKey);

            List<NotificationDelegationMetadataEntity> oneQueryResultList = oneQueryResult.getResults();
            log.info("batch deleting queried delegated notification");

            notificationDelegationMetadataEntityDao.batchDeleteItems(oneQueryResultList);

        } while(oneQueryResult.getLastEvaluatedKey() != null);
    }

    public List<NotificationDelegationMetadataEntity> computeDelegationMetadataEntries(NotificationMetadataEntity metadata) {
        List<InternalMandateDto> mandates = getMandatesByDelegator(metadata.getRecipientId(), null);
        return computeDelegationMetadataEntries(metadata, mandates).toList();
    }

    private Stream<NotificationDelegationMetadataEntity> computeDelegationMetadataEntries(NotificationMetadataEntity metadata, List<InternalMandateDto> mandates) {
        String creationMonth = DataUtils.extractCreationMonth(metadata.getSentAt());
        return mandates.stream()
                .flatMap(mandate -> {
                    if (CollectionUtils.isEmpty(mandate.getGroups())) {
                        return Stream.of(buildOneSearchDelegationMetadataEntry(metadata, mandate, null, creationMonth));
                    } else {
                        return mandate.getGroups().stream()
                                .map(g -> buildOneSearchDelegationMetadataEntry(metadata, mandate, g, creationMonth));
                    }
                });
    }

    private NotificationDelegationMetadataEntity buildOneSearchDelegationMetadataEntry(NotificationMetadataEntity metadata,
                                                                                       InternalMandateDto mandate,
                                                                                       String group,
                                                                                       String creationMonth) {
        String pk;
        String delegateIdGroupIdCreationMonth = null;
        if (StringUtils.hasText(group)) {
            pk = DataUtils.createConcatenation(metadata.getIunRecipientId(), mandate.getDelegate(), group);
            delegateIdGroupIdCreationMonth = DataUtils.createConcatenation(mandate.getDelegate(), group, creationMonth);
        } else {
            pk = DataUtils.createConcatenation(metadata.getIunRecipientId(), mandate.getDelegate());
        }
        return NotificationDelegationMetadataEntity.builder()
                .iunRecipientIdDelegateIdGroupId(pk)
                .sentAt(metadata.getSentAt())
                .delegateIdCreationMonth(DataUtils.createConcatenation(mandate.getDelegate(), creationMonth))
                .delegateIdGroupIdCreationMonth(delegateIdGroupIdCreationMonth)
                .mandateId(mandate.getMandateId())
                .senderId(metadata.getSenderId())
                .recipientId(metadata.getRecipientId())
                .recipientIds(metadata.getRecipientIds())
                .notificationStatus(metadata.getNotificationStatus())
                .senderIdCreationMonth(metadata.getSenderIdCreationMonth())
                .recipientIdCreationMonth(metadata.getRecipientIdCreationMonth())
                .senderIdRecipientId(metadata.getSenderIdRecipientId())
                .tableRow(metadata.getTableRow())
                .build();
    }

    private List<InternalMandateDto> getMandatesByDelegator(String delegatorId, String mandateId) {
        return mandateClient.listMandatesByDelegator(delegatorId, mandateId, null, null, null, DelegateType.PG);
    }

    private Optional<InternalMandateDto> getMandateByDelegator(String delegatorId, String mandateId) {
        List<InternalMandateDto> mandates = getMandatesByDelegator(delegatorId, mandateId);
        return mandates.stream()
                .filter(m -> mandateId.equals(m.getMandateId()))
                .findAny();
    }

    private PnLastEvaluatedKey lastEvaluatedKeyFromPage(PageSearchTrunk<NotificationMetadataEntity> page, String partition) {
        if (page.getLastEvaluatedKey() != null) {
            PnLastEvaluatedKey lastEvaluatedKey = new PnLastEvaluatedKey();
            lastEvaluatedKey.setExternalLastEvaluatedKey(partition);
            lastEvaluatedKey.setInternalLastEvaluatedKey(page.getLastEvaluatedKey());
            return lastEvaluatedKey;
        }
        return null;
    }
}
