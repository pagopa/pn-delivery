package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.BaseTest;
import it.pagopa.pn.delivery.exception.PnConflictException;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationReworksEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class NotificationReworksDaoDynamoTestIT extends BaseTest.WithLocalStack {

  @Autowired
  NotificationReworksDaoDynamo notificationReworksDaoDynamo;

  @Test
  void putAndGetItem() {
    NotificationReworksEntity entity = new NotificationReworksEntity();
    entity.setIun("IUN_DI_PROVA");
    entity.setReworkId("REWORKID_1");

    notificationReworksDaoDynamo.putIfAbsent(entity).block();

    NotificationReworksEntity result = notificationReworksDaoDynamo.findByIunAndReworkId("IUN_DI_PROVA", "REWORKID_1").block();

    assert result != null;
    Assertions.assertEquals("IUN_DI_PROVA", result.getIun());
    Assertions.assertEquals("REWORKID_1", result.getReworkId());
  }

  @Test
  void putAndGetItems() {
    NotificationReworksEntity entity = new NotificationReworksEntity();
    entity.setIun("IUN_DI_PROVA");
    entity.setReworkId("REWORKID_2");

    NotificationReworksEntity entity2 = new NotificationReworksEntity();
    entity2.setIun("IUN_DI_PROVA");
    entity2.setReworkId("REWORKID_3");

    notificationReworksDaoDynamo.putIfAbsent(entity).block();
    notificationReworksDaoDynamo.putIfAbsent(entity2).block();

    Mono<Page<NotificationReworksEntity>> result = notificationReworksDaoDynamo.findByIun("IUN_DI_PROVA", new HashMap<>(), 5);
    Page<NotificationReworksEntity> entities = result.block();

    assert result != null;
    Assertions.assertEquals("IUN_DI_PROVA", entities.items().get(0).getIun());
    Assertions.assertEquals("REWORKID_1", entities.items().get(0).getReworkId());

    Assertions.assertEquals("IUN_DI_PROVA", entities.items().get(1).getIun());
    Assertions.assertEquals("REWORKID_2", entities.items().get(1).getReworkId());
  }

  @Test
  void putIfAbsentConflict() {
    NotificationReworksEntity entity = new NotificationReworksEntity();
    entity.setIun("IUN_DI_PROVA");
    entity.setReworkId("REWORKID_1");
    
    Assertions.assertThrows(PnConflictException.class, () -> notificationReworksDaoDynamo.putIfAbsent(entity).block());
  }
}
