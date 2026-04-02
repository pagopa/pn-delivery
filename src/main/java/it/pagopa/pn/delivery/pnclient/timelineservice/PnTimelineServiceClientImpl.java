package it.pagopa.pn.delivery.pnclient.timelineservice;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.generated.openapi.msclient.timelineservice.v1.api.TimelineControllerApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.timelineservice.v1.model.DeliveryInformationResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
public class PnTimelineServiceClientImpl {

    private final TimelineControllerApi timelineControllerApi;

    public DeliveryInformationResponse getDeliveryInformation(String iun, Integer recIndex){
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_TIMELINE_SERVICE, "getDeliveryInformation");
        return timelineControllerApi.getDeliveryInformation(iun, recIndex);
    }
}
