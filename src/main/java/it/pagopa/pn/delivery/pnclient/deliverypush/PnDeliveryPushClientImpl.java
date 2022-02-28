package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Slf4j
@Component
public class PnDeliveryPushClientImpl implements PnDeliveryPushClient {
    private final RestTemplate restTemplate;
    private final PnDeliveryConfigs cfg;

    private static final String TIMELINE_PATH ="timelines";

    public PnDeliveryPushClientImpl(RestTemplate restTemplate, PnDeliveryConfigs cfg) {
        this.restTemplate = restTemplate;
        this.cfg = cfg;
    }

    public Set<TimelineElement> getTimelineElements(String iun) {
        log.debug("Start getTimelineElements for iun {}", iun);

        String url = cfg.getDeliveryPushBaseUrl() + "/" +TIMELINE_PATH +"/" + iun;

        log.info("url {}",url);

        ResponseEntity<Set<TimelineElement>> response = restTemplate.exchange(url,
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
