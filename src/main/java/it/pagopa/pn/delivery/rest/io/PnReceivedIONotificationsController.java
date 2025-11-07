package it.pagopa.pn.delivery.rest.io;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.exception.*;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.api.AppIoPnNotificationApi;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.RequestCheckQrMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ResponseCheckQrMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ThirdPartyMessage;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.NotificationQRService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.io.IOMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_CTX_TOPIC;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_IUN_KEY;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PnReceivedIONotificationsController implements AppIoPnNotificationApi {

    private final NotificationRetrieverService retrieveSvc;
    private final NotificationQRService notificationQRService;
    private final IOMapper ioMapper;

    @Override
    public ResponseEntity<ThirdPartyMessage> getReceivedNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaPnSrcCh, String iun, List<String> xPagopaPnCxGroups, String xPagopaPnSrcChDetails, UUID mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        ThirdPartyMessage result;
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VIEW_RCP, "getReceivedNotification")
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnUid, xPagopaPnCxGroups, xPagopaPnSrcCh, xPagopaPnSrcChDetails);
            InternalNotification internalNotification = retrieveSvc.getNotificationAndNotifyViewedEvent(iun, internalAuthHeader, mandateId != null ? mandateId.toString() : null);
            boolean isNotificationCancelled = retrieveSvc.isNotificationCancelled(internalNotification);
            result = ioMapper.mapToThirdPartMessage(internalNotification, isNotificationCancelled);
            logEvent.generateSuccess().log();
        } catch (PnMandateNotFoundException exc) {
            logEvent.generateFailure("Mandate not found: " + exc.getProblem().getDetail()).log();
            throw new PnBadRequestException("Mandate not found", "Mandate not found for iun = " + iun, PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_MANDATENOTFOUND);
        } catch (PnRootIdNonFountException exc) {
            logEvent.generateFailure("RootId not found: " + exc.getProblem().getDetail()).log();
            throw new PnBadRequestException("RootId not found", "RootId not found not found for iun = " + iun, PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_ROOTIDNOTFOUND);
        } catch (PnNotFoundException exc){
            logEvent.generateFailure("Notification not found: " + exc.getProblem().getDetail()).log();
            throw new PnBadRequestException("Notification not found", "Notification not found for iun = " + iun, PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<ResponseCheckQrMandateDto> checkAarQrCodeIO(CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaCxTaxid, RequestCheckQrMandateDto requestCheckQrMandateDto, List<String> xPagopaPnCxGroups)  {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        String aarQrCodeValue = requestCheckQrMandateDto.getAarQrCodeValue();
        String recipientType = xPagopaPnCxType.getValue();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before( PnAuditLogEventType.AUD_NT_REQQR, "checkAarQrCodeIO aarQrCodeValue={} recipientType={} customerId={}",
                        aarQrCodeValue,
                        recipientType,
                        xPagopaPnCxId)
                .mdcEntry(MDC_PN_CTX_TOPIC, String.format("aarQrCodeValue=%s", aarQrCodeValue))
                .build();
        logEvent.log();
        ResponseCheckQrMandateDto responseCheckAarMandateDto;
        try {
            responseCheckAarMandateDto = notificationQRService.getNotificationByQRFromIOWithMandate( requestCheckQrMandateDto, xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnCxGroups );
            logEvent.getMdc().put(MDC_PN_IUN_KEY, responseCheckAarMandateDto.getIun());
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("Exception on get notification by qr from IO= " + exc.getProblem()).log();
            throw exc;
        }

        return ResponseEntity.ok( responseCheckAarMandateDto );
    }
}
