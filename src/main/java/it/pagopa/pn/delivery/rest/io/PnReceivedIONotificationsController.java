package it.pagopa.pn.delivery.rest.io;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.appio.v1.api.AppIoPnNotificationApi;
import it.pagopa.pn.delivery.generated.openapi.appio.v1.dto.IOReceivedNotification;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PnReceivedIONotificationsController implements AppIoPnNotificationApi {

    private final NotificationRetrieverService retrieveSvc;
    private final ModelMapperFactory modelMapperFactory;

    @Override
    public ResponseEntity<IOReceivedNotification> getReceivedNotification(String iun, String xPagopaCxTaxid) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        IOReceivedNotification result;
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VIEW_RCP, "getReceivedNotification")
                .cxId(xPagopaCxTaxid)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalNotification internalNotification = retrieveSvc.getNotificationAndNotifyViewedEvent(iun, xPagopaCxTaxid, null);

            ModelMapper mapper = modelMapperFactory.createModelMapper(InternalNotification.class, IOReceivedNotification.class);

            result = mapper.map(internalNotification, IOReceivedNotification.class);

            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }
        return ResponseEntity.ok(result);
    }
}
