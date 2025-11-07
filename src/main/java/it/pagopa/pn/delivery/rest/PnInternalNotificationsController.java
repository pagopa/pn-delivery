package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.*;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_CTX_TOPIC;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_IUN_KEY;

@Slf4j
@RestController
public class PnInternalNotificationsController implements InternalOnlyApi {

    private final NotificationRetrieverService retrieveSvc;
    private final NotificationPriceService priceService;
    private final NotificationQRService qrService;
    private final NotificationAttachmentService notificationAttachmentService;
    private final ModelMapper modelMapper;


    public PnInternalNotificationsController(NotificationRetrieverService retrieveSvc, NotificationPriceService priceService, NotificationQRService qrService, NotificationAttachmentService notificationAttachmentService, ModelMapper modelMapper) {
        this.retrieveSvc = retrieveSvc;
        this.priceService = priceService;
        this.qrService = qrService;
        this.notificationAttachmentService = notificationAttachmentService;
        this.modelMapper = modelMapper;
    }


    @Override
    public ResponseEntity<UserInfoQrCode> decodeAarToken(RequestDecodeQrDto requestDecodeQrDto) {
        String aarQrCodeValue = requestDecodeQrDto.getAarTokenValue();
        log.info("Start decodeAarQrCode with aarQrCodeValue={}", aarQrCodeValue);
        UserInfoQrCode response;
        try {
            response = qrService.getAarQrCodeToDecode(requestDecodeQrDto);
            log.info("decodeAarQrCode success with aarQrCodeValue={}", aarQrCodeValue);
        } catch (PnNotFoundException exception) {
            log.error("Error in decodeAarQrCode for aarQrCodeValue={}: {}", aarQrCodeValue, exception.getMessage());
            throw exception;
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<NotificationCostResponse> getNotificationCostPrivate(String paTaxId, String noticeCode) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_REQCOST, "getNotificationCostPrivate paTaxId={} noticeCode={}", paTaxId, noticeCode)
                .mdcEntry(MDC_PN_CTX_TOPIC, String.format("paTaxId=%s;noticeCode=%s", paTaxId, noticeCode))
                .build();
        logEvent.log();
        NotificationCostResponse response;
        try {
            response = priceService.getNotificationCost( paTaxId, noticeCode );
            String iun = response.getIun();
            logEvent.getMdc().put(MDC_PN_IUN_KEY, iun);
            logEvent.generateSuccess("getNotificationCostPrivate paTaxId={}, noticeCode={}, iun={}, recipientIdx={}", paTaxId, noticeCode, iun, response.getRecipientIdx()).log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("Exception on get notification cost private= " + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok( response );
    }

    @Override
    public ResponseEntity<ResponseCheckAarDto> checkAarQrCode(RequestCheckAarDto requestCheckAarDto) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        String aarQrCodeValue = requestCheckAarDto.getAarQrCodeValue();
        String recipientType = requestCheckAarDto.getRecipientType();
        String recipientInternalId = requestCheckAarDto.getRecipientInternalId();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_REQQR, "getNotificationQrPrivate aarQrCodeValue={} recipientType={} recipientInternalId={}",
                        aarQrCodeValue,
                        recipientType,
                        recipientInternalId)
                .mdcEntry(MDC_PN_CTX_TOPIC, String.format("aarQrCodeValue=%s;recipientType=%s;recipientInternalId=%s", aarQrCodeValue, recipientType, recipientInternalId))
                .build();
        logEvent.log();
        ResponseCheckAarDto responseCheckAarDto;
        try {
            responseCheckAarDto = qrService.getNotificationByQR( requestCheckAarDto );
            logEvent.getMdc().put(MDC_PN_IUN_KEY, responseCheckAarDto.getIun());
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("Exception on get notification qr private= " + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok( responseCheckAarDto );
    }

    @Override
    public ResponseEntity<SentNotificationV25> getSentNotificationPrivate(String iun) {
        InternalNotification notification = retrieveSvc.getNotificationInformation(iun, false, true);
        SentNotificationV25 sentNotification = modelMapper.map(notification, SentNotificationV25.class);

        int recIdx = 0;
        for (NotificationRecipientV24 rec : sentNotification.getRecipients()) {
            rec.setInternalId(notification.getRecipientIds().get(recIdx));
            recIdx += 1;
        }

        return ResponseEntity.ok(sentNotification);
    }

    @Override
    public  ResponseEntity<NotificationSearchResponse> searchNotificationsPrivate(OffsetDateTime startDate, OffsetDateTime endDate,
                                                                                  String recipientId, Boolean recipientIdOpaque,
                                                                                  String senderId, List<NotificationStatusV26> status,
                                                                                  String mandateId, String cxType, Integer size, String nextPagesKey) {

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_SEARCH_SND, "searchNotificationsPrivate")
                .build();
        logEvent.log();
        if (StringUtils.hasText( recipientId ) && StringUtils.hasText( senderId )) {
            throw new IllegalArgumentException( "Please specify alternatively recipientId or senderId search params" );
        }
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .bySender( StringUtils.hasText( senderId ) )
                .senderReceiverId( StringUtils.hasText( recipientId )? recipientId : senderId)
                .mandateId(mandateId)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .statuses(status==null?List.of():status)
                .receiverIdIsOpaque(recipientIdOpaque)
                .size(size)
                .maxPageNumber( 1 )
                .nextPagesKey(nextPagesKey)
                .build();
        ResultPaginationDto<NotificationSearchRow,String> serviceResult;
        NotificationSearchResponse response = new NotificationSearchResponse();
        try {
            serviceResult = retrieveSvc.searchNotification(searchDto, cxType, null);
            response = modelMapper.map( serviceResult, NotificationSearchResponse.class );
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok( response );

    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedNotificationAttachmentPrivate(String iun, String attachmentName, String recipientInternalId, String mandateId, Integer attachmentIdx) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEventType eventType = PnAuditLogEventType.AUD_NT_ATCHOPEN_RCP;
        String logMsg = "getReceivedNotificationAttachmentPrivate attachment name={} attachment index={}";
        if (StringUtils.hasText( mandateId )) {
            eventType = PnAuditLogEventType.AUD_NT_ATCHOPEN_DEL;
            logMsg = "getReceivedAndDelegatedNotificationAttachmentPrivate attachment name={}, attachment index={} and mandateId={}";
        }
        NotificationAttachmentDownloadMetadataResponse response = new NotificationAttachmentDownloadMetadataResponse();
        PnAuditLogEvent logEvent = auditLogBuilder.before(eventType, logMsg, attachmentName, attachmentIdx, mandateId)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader("PF", recipientInternalId, null, null);
            response = notificationAttachmentService.downloadAttachmentWithRedirect(
                    iun,
                    internalAuthHeader,
                    mandateId,
                    null,
                    attachmentName,
                    attachmentIdx,
                    false
            );
            String fileName = response.getFilename();
            String url = response.getUrl();
            String retryAfter = String.valueOf( response.getRetryAfter() );
            String message = LogUtils.createAuditLogMessageForDownloadDocument(fileName, url, retryAfter);
            logEvent.generateSuccess("getReceivedNotificationAttachmentPrivate attachment name={} and attachment index={}, {}",
                    attachmentName, attachmentIdx, message).log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedNotificationDocumentPrivate(String iun, Integer docIdx, String recipientInternalId, String mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        NotificationAttachmentDownloadMetadataResponse response = new NotificationAttachmentDownloadMetadataResponse();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_DOCOPEN_RCP, "getReceivedNotificationDocumentPrivate from documents array with index={}", docIdx)
                .iun(iun)
                .build();
        logEvent.log();


        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader("PF", recipientInternalId, null, null);
            response = notificationAttachmentService.downloadDocumentWithRedirect(
                    iun,
                    internalAuthHeader,
                    mandateId,
                    docIdx,
                    false
            );
            String fileName = response.getFilename();
            String url = response.getUrl();
            String retryAfter = String.valueOf( response.getRetryAfter() );
            String message = LogUtils.createAuditLogMessageForDownloadDocument(fileName, url, retryAfter);
            logEvent.generateSuccess("getReceivedNotificationDocumentPrivate {}", message).log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Map<String, String>> getQuickAccessLinkTokensPrivate(String iun) {
      return ResponseEntity.ok(qrService.getQRByIun(iun));
    }

    @Override
    public ResponseEntity<Void> removeAllNotificationCostsByIun(String iun) {
        priceService.removeAllNotificationCostsByIun(iun);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> checkIUNAndInternalId(String iun, String recipientInternalId, String mandateId, String queryCxType, List<String> queryCxGroups) {
        retrieveSvc.checkIUNAndInternalId(iun, recipientInternalId, mandateId, queryCxType, queryCxGroups);
        return ResponseEntity.noContent().build();
    }
}
