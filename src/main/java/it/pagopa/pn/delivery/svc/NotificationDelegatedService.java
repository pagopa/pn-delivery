package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.EventType;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.*;

@Slf4j
@Service
public class NotificationDelegatedService {

    private final NotificationMetadataEntityDao notificationMetadataEntityDao;
    private final NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao;
    private final PnMandateClientImpl mandateClient;

    private static final int DEFAULT_DYNAMO_QUERY_SIZE = 1000;
    private static final int MAX_DYNAMO_QUERY_SIZE = 2000;

    public NotificationDelegatedService(NotificationMetadataEntityDao notificationMetadataEntityDao,
                                        NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao,
                                        PnMandateClientImpl mandateClient) {
        this.notificationMetadataEntityDao = notificationMetadataEntityDao;
        this.notificationDelegationMetadataEntityDao = notificationDelegationMetadataEntityDao;
        this.mandateClient = mandateClient;
    }


    /**
     * All'accettazione di una delega si esegue la duplicazione di tutte le notifiche ricevute dal delegante a partire
     * dall'inizio validità della delega fino al momento attuale (le notifiche ricevute successivamente vengono duplicate
     * dal flusso "standard" in update-status).
     *
     * @param event     evento di accettazione della delega
     * @param eventType tipo di evento
     */
    public void handleAcceptedMandate(PnMandateEvent.Payload event, EventType eventType) {
        log.info("handling {} mandate: {}, delegator={}, delegate={}", eventType, event.getMandateId(), event.getDelegatorId(), event.getDelegateId());
        InternalMandateDto mandate = getMandateByDelegator(event.getDelegatorId(), event.getMandateId())
                .orElseThrow(() -> new PnInternalException("Mandate not found", ERROR_CODE_DELIVERY_MANDATENOTFOUND));
        log.debug("mandate: {}", mandate);

        InputSearchNotificationDto searchDto = InputSearchNotificationDto.builder()
                .senderReceiverId(event.getDelegatorId())
                .bySender(false)
                .startDate(event.getValidFrom())
                .endDate(Instant.now()) // duplico solo le notifiche ricevute fino ad adesso, quelle successive verranno duplicate tramite il flusso standard
                .size(DEFAULT_DYNAMO_QUERY_SIZE)
                .build();
        log.debug("filters: {}", searchDto);

        chooseIndexAndDuplicateNotifications(searchDto, mandate.getGroups(), mandate);
    }

    private void chooseIndexAndDuplicateNotifications(InputSearchNotificationDto searchDto, List<String> groups, InternalMandateDto mandate) {
        IndexNameAndPartitions indexAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(searchDto);
        if (IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_RECEIVER != indexAndPartitions.getIndexName()) {
            log.error("index must be on receiverId instead of {}", indexAndPartitions.getIndexName());
            throw new PnInternalException("Index not valid", ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME);
        }
        log.debug("accepted mandate indexAndPartitions: {}", indexAndPartitions);

        duplicateNotifications(indexAndPartitions, groups, mandate, searchDto);
    }

    private void duplicateNotifications(IndexNameAndPartitions indexAndPartitions,
                                        List<String> groups,
                                        InternalMandateDto mandate,
                                        InputSearchNotificationDto searchDto) {
        int globalIterations = 0;
        int duplicationCounter = 0;

        for (String partition : indexAndPartitions.getPartitions()) {
            PnLastEvaluatedKey lastEvaluatedKey = null;
            int partitionIterations = 0;
            do {
                log.debug("querying notification by receiverId: {}, {} results at a time", searchDto.getSenderReceiverId(), DEFAULT_DYNAMO_QUERY_SIZE);
                PageSearchTrunk<NotificationMetadataEntity> page = notificationMetadataEntityDao.searchForOneMonth(searchDto,
                        IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_RECEIVER.getValue(),
                        partition,
                        DEFAULT_DYNAMO_QUERY_SIZE,
                        lastEvaluatedKey);
                List<NotificationDelegationMetadataEntity> entries = saveOnePageOfNotifications(page, groups, mandate);
                duplicationCounter += entries.size();
                lastEvaluatedKey = lastEvaluatedKeyFromPage(page, partition);
                log.debug("last evaluated key: {}", lastEvaluatedKey);
                partitionIterations++;
            } while (lastEvaluatedKey != null);
            globalIterations += partitionIterations;
            log.info("mandateId={} partition={} iterations={}", mandate.getMandateId(), partition, partitionIterations);
        }

        log.info("mandateId={} duplicationCounter={} globalIterations={}", mandate.getMandateId(), duplicationCounter, globalIterations);
    }

    private List<NotificationDelegationMetadataEntity> saveOnePageOfNotifications(PageSearchTrunk<NotificationMetadataEntity> page,
                                                                                  List<String> groups,
                                                                                  InternalMandateDto mandate) {
        List<NotificationDelegationMetadataEntity> entries = page.getResults().stream()
                .flatMap(metadata -> computeDelegationMetadataEntries(metadata, groups, List.of(mandate)))
                .toList();
        log.debug("saving {} delegation metadata entries", entries.size());
        List<NotificationDelegationMetadataEntity> unprocessed = notificationDelegationMetadataEntityDao.batchPutItems(entries);
        if (!unprocessed.isEmpty()) {
            log.error("can not save all delegation metadata entities - unprocessed entries = {}", unprocessed.size());
            throw new PnInternalException("Can not save all delegation metadata entities", ERROR_CODE_DELIVERY_HANDLEEVENTFAILED);
        }
        return entries;
    }

    public void handleUpdatedMandate(PnMandateEvent.Payload event, EventType eventType) {
        log.info("handling {} mandate: {}, delegator={}, delegate={}", eventType, event.getMandateId(), event.getDelegatorId(), event.getDelegateId());
        InternalMandateDto mandate = getMandateByDelegator(event.getDelegatorId(), event.getMandateId())
                .orElseThrow(() -> new PnInternalException("Mandate not found", ERROR_CODE_DELIVERY_MANDATENOTFOUND));
        log.debug("mandate: {}", mandate);

        if (!CollectionUtils.isEmpty(event.getAddedGroups())) {
            log.info("mandate {} has new groups {}, duplicating notifications", mandate.getMandateId(), event.getAddedGroups());
            List<String> groups = event.getAddedGroups() != null ? new ArrayList<>(event.getAddedGroups()) : Collections.emptyList();

            var now = Instant.now();
            InputSearchNotificationDto searchDto = InputSearchNotificationDto.builder()
                    .senderReceiverId(event.getDelegatorId())
                    .bySender(false)
                    .startDate(now.minus(120, ChronoUnit.DAYS))
                    .endDate(now) // duplico solo le notifiche ricevute fino ad adesso, quelle successive verranno duplicate tramite il flusso standard
                    .size(DEFAULT_DYNAMO_QUERY_SIZE)
                    .build();
            log.debug("filters: {}", searchDto);

            chooseIndexAndDuplicateNotifications(searchDto, groups, mandate);
        }
        if (!CollectionUtils.isEmpty(event.getRemovedGroups())) {
            log.info("mandate {} has removed groups {}, duplicating notifications", mandate.getMandateId(), event.getRemovedGroups());

            deleteNotificationDelegatedByMandateIdAndGroups(event.getMandateId(), event.getRemovedGroups());
        }
    }

    /**
     * Quando una delega viene revocata, rifiutata o scade vengono cancellate tutte le notifiche duplicate per quella
     * delega.
     *
     * @param mandateId id della delega
     * @param eventType tipo di evento
     */
    public void deleteNotificationDelegatedByMandateId(String mandateId, EventType eventType) {
        log.info("handling {} mandate: {}", eventType, mandateId);

        deleteNotificationDelegatedByMandateIdAndGroups(mandateId, Collections.emptySet());
    }

    /**
     * Effettua la duplicazione della notifica recuperando le informazioni sulle deleghe.
     *
     * @param metadata  notifica da duplicare
     * @return          notifiche duplicate
     */
    public List<NotificationDelegationMetadataEntity> computeDelegationMetadataEntries(NotificationMetadataEntity metadata) {
        List<InternalMandateDto> mandates = getMandatesByDelegator(metadata.getRecipientId(), null);
        return computeDelegationMetadataEntries(metadata, null, mandates).toList();
    }

    private void deleteNotificationDelegatedByMandateIdAndGroups(String mandateId, Set<String> group) {
        PnLastEvaluatedKey startEvaluatedKey = new PnLastEvaluatedKey();
        PageSearchTrunk<NotificationDelegationMetadataEntity> oneQueryResult;

        int iterations = 0;
        int deleteCount = 0;
        int querySize = DEFAULT_DYNAMO_QUERY_SIZE;
        do {
            log.debug("querying delegated notification by mandateId: {}, {} results at a time", mandateId, DEFAULT_DYNAMO_QUERY_SIZE);
            oneQueryResult = notificationDelegationMetadataEntityDao.searchDelegatedByMandateId(mandateId, group, querySize, startEvaluatedKey);

            List<NotificationDelegationMetadataEntity> oneQueryResultList = oneQueryResult.getResults();
            log.info("batch deleting queried delegated notification: {}", oneQueryResult.getResults().size());
            deleteCount += oneQueryResult.getResults().size();

            querySize = adjustQuerySize(querySize, oneQueryResult.getResults().size());

            oneQueryResultList.forEach(notificationDelegationMetadataEntityDao::deleteWithConditions);

            iterations++;
            log.debug("last evaluated key: {}", oneQueryResult.getLastEvaluatedKey());
        } while (oneQueryResult.getLastEvaluatedKey() != null);
        log.info("mandateId={} deleteCount={} iterations={}", mandateId, deleteCount, iterations);
    }

    private Stream<NotificationDelegationMetadataEntity> computeDelegationMetadataEntries(NotificationMetadataEntity metadata,
                                                                                          List<String> groups,
                                                                                          List<InternalMandateDto> mandates) {
        String creationMonth = DataUtils.extractCreationMonth(metadata.getSentAt());
        return mandates.stream()
                .filter(mandate -> CollectionUtils.isEmpty(mandate.getVisibilityIds())
                        || mandate.getVisibilityIds().contains(metadata.getSenderId()))
                .flatMap(mandate -> {
                    Stream<NotificationDelegationMetadataEntity> entries = Stream.empty();
                    if (!CollectionUtils.isEmpty(groups)) {
                        entries = groups.stream().map(g -> buildOneSearchDelegationMetadataEntry(metadata, mandate, g, creationMonth));
                    }
                    // Alla duplicazione viene aggiunto sempre un elemento senza gruppo; questo è necessario per
                    // consentire all'amministratore di vedere tutte le notifiche ricevute dall'inizio della delega.
                    // Quando i gruppi variano nel tempo, questo elemento rimane e viene sempre aggiornato.
                    return Stream.concat(entries, Stream.of(buildOneSearchDelegationMetadataEntry(metadata, mandate, null, creationMonth)));
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
        log.debug("mandates with mandateId={} and delegatorId={}: {}", mandateId, delegatorId, mandates.size());
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

    private int adjustQuerySize(int currentQuerySize, int resultSize) {
        float multiplier = 2 - Math.min(resultSize / (float) DEFAULT_DYNAMO_QUERY_SIZE, 1);
        int newQuerySize = Math.round(currentQuerySize * multiplier);
        return Math.min(newQuerySize, MAX_DYNAMO_QUERY_SIZE);
    }
}
