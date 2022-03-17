package it.pagopa.pn.delivery.middleware.notificationdao;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons_delivery.model.notification.cassandra.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.cql.QueryOptions;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = NotificationDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.CASSANDRA)
@Slf4j
@Deprecated
public class CassandraNotificationDao implements NotificationDao {

    private final CassandraOperations cassandraTemplate;
    private final KeyValueStore<String, NotificationEntity> notificationEntityDao;
    private final KeyValueStore<NotificationBySenderEntityId, NotificationBySenderEntity> notificationBySenderEntityDao;
    private final KeyValueStore<NotificationByRecipientEntityId, NotificationByRecipientEntity> notificationByRecipientEntityDao;
    private final DtoToEntityNotificationMapper dto2entityMapper;
    private final DtoToSearchEntityMapper dto2SearchEntityMapper;
    private final EntityToDtoNotificationMapper entity2dtoMapper;

    public CassandraNotificationDao(
            CassandraOperations cassandraTemplate,
            KeyValueStore<String, NotificationEntity> notificationEntityDao,
            KeyValueStore<NotificationBySenderEntityId, NotificationBySenderEntity> notificationBySenderEntityDao,
            KeyValueStore<NotificationByRecipientEntityId, NotificationByRecipientEntity> notificationByRecipientEntityDao,
            DtoToEntityNotificationMapper dto2entityMapper,
            DtoToSearchEntityMapper dto2SearchEntityMapper,
            EntityToDtoNotificationMapper entity2dtoMapper) {
        this.cassandraTemplate = cassandraTemplate;
        this.notificationEntityDao = notificationEntityDao;
        this.notificationBySenderEntityDao = notificationBySenderEntityDao;
        this.notificationByRecipientEntityDao = notificationByRecipientEntityDao;
        this.dto2entityMapper = dto2entityMapper;
        this.dto2SearchEntityMapper = dto2SearchEntityMapper;
        this.entity2dtoMapper = entity2dtoMapper;
    }

    @Override
    public void addNotification(Notification notification) throws IdConflictException {
        dto2SearchEntityMapper
                .dto2SenderEntity(notification, NotificationStatus.IN_VALIDATION)
                .forEach( notificationBySenderEntityDao::put );
        
        dto2SearchEntityMapper
                .dto2RecipientEntity(notification, NotificationStatus.IN_VALIDATION)
                .forEach( notificationByRecipientEntityDao::put );
        
        it.pagopa.pn.delivery.middleware.model.notification.NotificationEntity entity = dto2entityMapper.dto2Entity(notification);
        //notificationEntityDao.putIfAbsent(entity);
    }

    @Override
    public Optional<Notification> getNotificationByIun(String iun) {
        return Optional.empty();
    }


    public List<NotificationSearchRow> searchNotification(InputSearchNotificationDto searchDto) {
        Predicate<String> matchSubject = buildRegexpPredicate(searchDto.getSubjectRegExp());
        Predicate<String> matchFilter = buildEqualsPredicate(searchDto.getFilterId());
        Predicate<String> matchIun = buildEqualsPredicate( searchDto.getIunMatch() );

        List<NotificationSearchRow> result;
        if (searchDto.getStatus() != null) {
            result = executeSearchNotificationQuery(searchDto);
        } else {
            result = new ArrayList<>();
            for (NotificationStatus oneStatus : NotificationStatus.values()) {
                searchDto.setStatus(oneStatus);
                List<NotificationSearchRow> oneStatusResult = executeSearchNotificationQuery(searchDto);
                result.addAll(oneStatusResult);
            }
        }
        
        return Collections.emptyList(); /*result.stream()
                .filter(row -> matchFilter.test(searchDto.isBySender() ? row.getRecipientId() : row.getSenderId()))
                .filter(row -> matchSubject.test(row.getSubject()))
                .filter(row -> matchIun.test(row.getIun()))
                .sorted(Comparator.comparing(NotificationSearchRow::getSentAt))
                .collect(Collectors.toList());*/
    }

    Predicate<String> buildRegexpPredicate(String subjectRegExp) {
        Predicate<String> matchSubject;
        if (subjectRegExp != null) {
            matchSubject = Objects::nonNull;
            matchSubject = matchSubject.and(Pattern.compile("^" + subjectRegExp + "$").asMatchPredicate());
        } else {
            matchSubject = x -> true;
        }
        return matchSubject;
    }


    private Predicate<String> buildEqualsPredicate(String filter) {
        Predicate<String> matchPredicate;
        if (filter != null) {
            matchPredicate = filter::equals;
        } else {
            matchPredicate = x -> true;
        }
        return matchPredicate;
    }

    private List<NotificationSearchRow> executeSearchNotificationQuery(InputSearchNotificationDto searchDto) {
        Query query = generateSearchNotificationQuery(searchDto);

        if (searchDto.isBySender()) {
            return cassandraTemplate.select(query, NotificationBySenderEntity.class)
                    .stream().map(entity -> NotificationSearchRow.builder()
                            .iun(entity.getNotificationBySenderId().getIun())
                            .sentAt(entity.getNotificationBySenderId().getSentat())
                            .senderId(entity.getNotificationBySenderId().getSenderId())
                            .notificationStatus(entity.getNotificationBySenderId().getNotificationStatus())
                            //.recipientId(entity.getNotificationBySenderId().getRecipientId())
                            .paNotificationId(entity.getPaNotificationId())
                            .subject(entity.getSubject())
                            .build()
                    )
                    .collect(Collectors.toList());
        } else {
            return cassandraTemplate.select(query, NotificationByRecipientEntity.class)
                    .stream().map(entity -> NotificationSearchRow.builder()
                            .iun(entity.getNotificationByRecipientId().getIun())
                            .sentAt(entity.getNotificationByRecipientId().getSentat())
                            .senderId(entity.getNotificationByRecipientId().getSenderId())
                            .notificationStatus(entity.getNotificationByRecipientId().getNotificationStatus())
                            //.recipientId(entity.getNotificationByRecipientId().getRecipientId())
                            .paNotificationId(entity.getPaNotificationId())
                            .subject(entity.getSubject())
                            .build()
                    )
                    .collect(Collectors.toList());
        }
    }

    private Query generateSearchNotificationQuery(InputSearchNotificationDto searchDto) {
        String entityIdProperty = searchDto.isBySender() ? "notificationBySenderId" : "notificationByRecipientId";
        String senderReceiverProperty = entityIdProperty + "." + (searchDto.isBySender() ? "senderId" : "recipientId");
        return Query.query(
                Criteria.where(entityIdProperty + ".notificationStatus").is(searchDto.getStatus()),
                Criteria.where(senderReceiverProperty).is(searchDto.getSenderReceiverId()),
                Criteria.where(entityIdProperty + ".sentat").gte(searchDto.getStartDate()),
                Criteria.where(entityIdProperty + ".sentat").lte(searchDto.getEndDate())
        ).queryOptions(QueryOptions.builder()
                .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                .build()
        );
    }
}
