package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import software.amazon.awssdk.enhanced.dynamodb.Key;

public interface NotificationDelegationMetadataEntityDao extends KeyValueStore<Key, NotificationDelegationMetadataEntity> {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";

    PageSearchTrunk<NotificationDelegationMetadataEntity> searchForOneMonth();

    PageSearchTrunk<NotificationDelegationMetadataEntity> searchByIun(InputSearchNotificationDto inputSearchNotificationDto,
                                                                      String pk,
                                                                      String sk);

}