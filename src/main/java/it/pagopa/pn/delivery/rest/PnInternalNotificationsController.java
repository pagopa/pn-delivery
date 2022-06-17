package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
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

import java.util.Map;

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
        String logMessage = String.format(
                "Update status for iun %s netxStatus %s", requestUpdateStatusDto.getIun(), requestUpdateStatusDto.getNextStatus()
        );
        log.info(logMessage);
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_STATUS, "updateStatus")
                .iun(requestUpdateStatusDto.getIun())
                .build();
        try {
            statusService.updateStatus(requestUpdateStatusDto);
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(logMessage).log();
            throw exc;
        }
        return ResponseEntity.ok().build();
    }
}
