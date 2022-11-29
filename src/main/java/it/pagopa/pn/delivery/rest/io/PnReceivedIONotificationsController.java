package it.pagopa.pn.delivery.rest.io;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.appio.v1.api.AppIoPnNotificationApi;
import it.pagopa.pn.delivery.generated.openapi.appio.v1.dto.ThirdPartyMessage;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PnReceivedIONotificationsController implements AppIoPnNotificationApi {

    private final NotificationRetrieverService retrieveSvc;
    private final IOMapper ioMapper;

    @Override
    public ResponseEntity<ThirdPartyMessage> getReceivedNotification(String iun, String xPagopaCxTaxid) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        ThirdPartyMessage result;
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VIEW_RCP, "getReceivedNotification")
                .cxId(xPagopaCxTaxid)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalNotification internalNotification = retrieveSvc.getNotificationAndNotifyViewedEvent(iun, xPagopaCxTaxid, null);
            result = ioMapper.mapToThirdPartMessage(internalNotification);
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }
        return ResponseEntity.ok(result);
    }
}
