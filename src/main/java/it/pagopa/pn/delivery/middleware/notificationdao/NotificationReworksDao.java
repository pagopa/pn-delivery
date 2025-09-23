package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationReworksEntity;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface NotificationReworksDao {
    Mono<NotificationReworksEntity> putItem(NotificationReworksEntity notificationReworksEntity);
    Mono<NotificationReworksEntity> findByIunAndReworkId(String iun, String reworkId);
    Mono<Page<NotificationReworksEntity>> findByIun(String iun, Map<String, AttributeValue> lastEvaluateKey, int limit);
}
