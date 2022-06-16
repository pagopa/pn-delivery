package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.SentNotification;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.StatusService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class PnInternalNotificationsController implements InternalOnlyApi {

    private final NotificationRetrieverService retrieveSvc;
    private final StatusService statusService;
    private final PnDeliveryConfigs cfg;
    private final ModelMapperFactory modelMapperFactory;

    public PnInternalNotificationsController(NotificationRetrieverService retrieveSvc, StatusService statusService, PnDeliveryConfigs cfg, ModelMapperFactory modelMapperFactory) {
        this.retrieveSvc = retrieveSvc;
        this.statusService = statusService;
        this.cfg = cfg;
        this.modelMapperFactory = modelMapperFactory;
    }

    @Override
    public ResponseEntity<SentNotification> getSentNotificationPrivate(String iun) {
        InternalNotification notification = retrieveSvc.getNotificationInformation(iun, false);
        ModelMapper mapper = modelMapperFactory.createModelMapper(InternalNotification.class, SentNotification.class);
        SentNotification sentNotification = mapper.map(notification, SentNotification.class);

        int recIdx = 0;
        for (NotificationRecipient rec : sentNotification.getRecipients()) {
            rec.setTaxId(notification.getRecipientIds().get(recIdx));
            recIdx += 1;
        }
        return ResponseEntity.ok(sentNotification);
    }

    @Override
    public ResponseEntity<Void> updateStatus(RequestUpdateStatusDto requestUpdateStatusDto) {
        log.info("Starting Update status for iun={} nextStatus={}", requestUpdateStatusDto.getIun(), requestUpdateStatusDto.getNextStatus());
        statusService.updateStatus(requestUpdateStatusDto);
        return ResponseEntity.ok().build();
    }
}
