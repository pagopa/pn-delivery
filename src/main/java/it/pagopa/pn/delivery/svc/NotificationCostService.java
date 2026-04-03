package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.delivery.models.NotificationCostRequest;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;

public interface NotificationCostService {
    NotificationProcessCostResponseInt getNotificationCost(NotificationCostRequest request);
}
