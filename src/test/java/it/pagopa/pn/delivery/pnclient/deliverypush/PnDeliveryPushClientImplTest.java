package it.pagopa.pn.delivery.pnclient.deliverypush;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.timelinedao.EntityToDtoTimelineMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

class PnDeliveryPushClientImplTest {
    private PnDeliveryPushClient pnDeliveryPushClient;
    
    @BeforeEach
    public void setup(){
        pnDeliveryPushClient = new PnDeliveryPushClientImpl(new RestTemplate());
    }
    
    @Test
    void getTimelineElements() {
        String iun = "Varcaturo-2";
        Set<TimelineElement> elem =  pnDeliveryPushClient.getTimelineElements(iun);
    }
}