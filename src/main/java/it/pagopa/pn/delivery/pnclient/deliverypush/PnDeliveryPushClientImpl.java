package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.timelinedao.EntityToDtoTimelineMapper;
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
    private final String PN_DELIVERY_PUSH_BASE_URL ="http://localhost:8081/delivery-push";
    private final String TIMELINE_BY_IUN ="/timelines";

    public PnDeliveryPushClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Set<TimelineElement> getTimelineElements(String iun) {
        log.info("Start getTimelineElements for iun {}", iun);

        String url = PN_DELIVERY_PUSH_BASE_URL + TIMELINE_BY_IUN+"/" + iun;
        
        ResponseEntity<Set<TimelineElement>> response = restTemplate.exchange(url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {});

        log.info("response {}", response);

        if (response.getStatusCode().isError()) {
            throw new PnInternalException("Error calling url " + response + " status " + response.getStatusCodeValue());
        }

        return response.getBody();
    }
}
