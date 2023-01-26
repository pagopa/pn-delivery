package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.middleware.notificationdao.NotificationDelegationMetadataEntityDaoDynamo;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;

import java.util.List;

public class NotificationDelegatedService {
    private static final int QUERY_RESULTS_SIZE = 1000;
    private static final int MAX_DYNAMODB_BATCH_SIZE = 100;


    private final NotificationDelegationMetadataEntityDaoDynamo notificationDao;


    public NotificationDelegatedService(NotificationDelegationMetadataEntityDaoDynamo notificationDao) {
        this.notificationDao = notificationDao;
    }

    public void deleteNotificationDelegatedByMandateId(String mandateId) {
        PnLastEvaluatedKey startEvaluatedKey = new PnLastEvaluatedKey();
        PageSearchTrunk<NotificationDelegationMetadataEntity> oneQueryResult;

        do {
            oneQueryResult = notificationDao.searchDelegatedByMandateId(mandateId, QUERY_RESULTS_SIZE,
                    startEvaluatedKey);
            List<NotificationDelegationMetadataEntity> oneQueryResultList = oneQueryResult.getResults();

            for (int i = 0; i < oneQueryResult.getResults().size(); i = i + MAX_DYNAMODB_BATCH_SIZE)
            {
                List<NotificationDelegationMetadataEntity> deleteBatchItems = oneQueryResultList
                        .subList(i, Math.min(oneQueryResultList.size(), i + MAX_DYNAMODB_BATCH_SIZE));
                notificationDao.batchDeleteNotificationDelegated(deleteBatchItems);
            }
        } while(oneQueryResult.getLastEvaluatedKey() != null);
    }
}
