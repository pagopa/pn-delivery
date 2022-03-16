package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.model.notification.NotificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = NotificationDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class NotificationDaoDynamo implements NotificationDao {

    private final NotificationEntityDao<Key,NotificationEntity> entityDao;
    private final DtoToEntityNotificationMapper dto2entityMapper;
    private final EntityToDtoNotificationMapper entity2DtoMapper;

    public NotificationDaoDynamo(
            NotificationEntityDao<Key,NotificationEntity> entityDao,
            DtoToEntityNotificationMapper dto2entityMapper,
            EntityToDtoNotificationMapper entity2DtoMapper) {
        this.entityDao = entityDao;
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
    public List<NotificationSearchRow> searchNotification(InputSearchNotificationDto inputSearchNotificationDto) {
        return null;
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
