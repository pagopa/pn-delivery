package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.model.notification.NotificationEntity;
import it.pagopa.pn.delivery.middleware.model.notification.NotificationMetadataEntity;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = NotificationDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class NotificationDaoDynamo implements NotificationDao {

    private final NotificationEntityDao entityDao;
    private final NotificationMetadataEntityDao<Key,NotificationMetadataEntity> metadataEntityDao;
    private final DtoToEntityNotificationMapper dto2entityMapper;
    private final EntityToDtoNotificationMapper entity2DtoMapper;

    public NotificationDaoDynamo(
            NotificationEntityDao entityDao,
            NotificationMetadataEntityDao<Key, NotificationMetadataEntity> metadataEntityDao, DtoToEntityNotificationMapper dto2entityMapper,
            EntityToDtoNotificationMapper entity2DtoMapper) {
        this.entityDao = entityDao;
        this.metadataEntityDao = metadataEntityDao;
        this.dto2entityMapper = dto2entityMapper;
        this.entity2DtoMapper = entity2DtoMapper;
    }

    @Override
    public void addNotification(Notification notification) throws IdConflictException {

        NotificationEntity entity = dto2entityMapper.dto2Entity( notification );
        entityDao.putIfAbsent( entity );
    }

    @Override
    public Optional<Notification> getNotificationByIun(String iun) {
        Key keyToSearch = Key.builder()
                .partitionValue(iun)
                .build();
        return entityDao.get( keyToSearch )
                .map( entity2DtoMapper::entity2Dto );
    }

    @Override
    public ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchNotification(InputSearchNotificationDto inputSearchNotificationDto, PnLastEvaluatedKey lastEvaluatedKey) {
        return metadataEntityDao.searchNotificationMetadata( inputSearchNotificationDto, lastEvaluatedKey );
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
}
