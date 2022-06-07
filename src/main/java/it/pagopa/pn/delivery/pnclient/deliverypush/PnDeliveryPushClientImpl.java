package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.api.TimelineAndStatusApi;
import it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationHistoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;

@Slf4j
@Component
public class PnDeliveryPushClientImpl {

    private final TimelineAndStatusApi timelineAndStatusApi;

    public PnDeliveryPushClientImpl(@Qualifier("withTracing")RestTemplate restTemplate, PnDeliveryConfigs cfg) {
        ApiClient newApiClient = new ApiClient( restTemplate );
        newApiClient.setBasePath(cfg.getDeliveryPushBaseUrl());
        this.timelineAndStatusApi = new TimelineAndStatusApi( newApiClient );
    }

    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, OffsetDateTime createdAt) {
    	log.debug("Starting getTimelineAndStatusHistory with iun={}, numberOfRecipients={} and createdAt={}", iun, numberOfRecipients, createdAt);
    	return timelineAndStatusApi.getNotificationHistory( iun, numberOfRecipients, createdAt);
    }
}
