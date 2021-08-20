package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty( name = NotificationDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.FAKE )
@Slf4j
public class FakeNotificationDao implements NotificationDao {

    private Map<String, Notification> storage = new ConcurrentHashMap<>();

    public FakeNotificationDao() {
        log.info("FAKE KeyValueStore !!!!!!!");
    }

    @Override
    public void addNotification(Notification notification) throws IdConflictException {
        if ( this.storage.putIfAbsent( notification.getIun(), notification ) != null ) {
            throw new IdConflictException( notification.getIun() );
        }
    }

    @Override
    public Notification getNotificationByIun(String iun) {
        return this.storage.get( iun );
    }

    @Override
    public void deleteNotificationByIun(String iun) {
        this.storage.remove( iun );
    }

}
