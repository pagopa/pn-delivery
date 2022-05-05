package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineStatusHistoryDto;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
public class PnDeliveryPushClientImpl implements PnDeliveryPushClient {
    private final RestTemplate restTemplate;
    private final PnDeliveryConfigs cfg;

    private static final String TIMELINE_AND_HISTORY_PATH ="timeline-and-history";

    public PnDeliveryPushClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
        this.restTemplate = restTemplate;
        this.cfg = cfg;
    }
    
    @Override
    public TimelineStatusHistoryDto getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        log.debug("Start getTimelineElements for iun {}", iun);

        String url = cfg.getDeliveryPushBaseUrl() + "/" + TIMELINE_AND_HISTORY_PATH +"/" + iun +"/" + numberOfRecipients +"/" + createdAt;

        log.info("url {}",url);

        ResponseEntity<TimelineStatusHistoryDto> response = restTemplate.exchange(url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {});

        if (response.getStatusCode().isError()) {
            log.error("Error calling url " + response + " status " + response.getStatusCodeValue());
            throw new PnInternalException("Error calling url " + response + " status " + response.getStatusCodeValue());
        }

        log.debug("getTimelineElements complete for iun {}", iun);

        return response.getBody();
    }
}
