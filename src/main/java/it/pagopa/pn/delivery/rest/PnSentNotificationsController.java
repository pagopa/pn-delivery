package it.pagopa.pn.delivery.rest;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadB2BApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadWebApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
public class PnSentNotificationsController implements SenderReadB2BApi,SenderReadWebApi {

    private final NotificationRetrieverService retrieveSvc;
    private final NotificationAttachmentService notificationAttachmentService;
    private final ModelMapperFactory modelMapperFactory;
    public static final String VALIDATION_ERROR_STATUS = "Validation error";

    public PnSentNotificationsController(NotificationRetrieverService retrieveSvc, NotificationAttachmentService notificationAttachmentService, ModelMapperFactory modelMapperFactory) {
        this.retrieveSvc = retrieveSvc;
        this.notificationAttachmentService = notificationAttachmentService;
        this.modelMapperFactory = modelMapperFactory;
    }

    @Override
    public ResponseEntity<FullSentNotification> getSentNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, List<String> xPagopaPnCxGroups) {
        InternalNotification internalNotification = retrieveSvc.getNotificationInformation( iun, true );
        ModelMapper mapper = modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class );
        FullSentNotification result = mapper.map( internalNotification, FullSentNotification.class );
        return ResponseEntity.ok( result );
    }



    @Override
    public ResponseEntity<NotificationSearchResponse> searchSentNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, Date startDate, Date endDate, List<String> xPagopaPnCxGroups, String recipientId, NotificationStatus status, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_SEARCH_SND, "searchSentNotification")
                .cxId(xPagopaPnCxId)
                .cxType(xPagopaPnCxType.toString())
                .iun(iunMatch)
                .uid(xPagopaPnUid)
                .build();
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(true)
                .senderReceiverId(xPagopaPnCxId)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .filterId(recipientId)
                .statuses(status==null?List.of():List.of(status))
                .receiverIdIsOpaque(false)
                //.groups( groups != null ? Arrays.asList( groups ) : null )
                .subjectRegExp(subjectRegExp)
                .iunMatch(iunMatch)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();
        ResultPaginationDto<NotificationSearchRow,String> serviceResult;
        NotificationSearchResponse response = new NotificationSearchResponse();
        try {
            serviceResult =  retrieveSvc.searchNotification( searchDto );
            ModelMapper mapper = modelMapperFactory.createModelMapper(ResultPaginationDto.class, NotificationSearchResponse.class );
            response = mapper.map( serviceResult, NotificationSearchResponse.class );
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }
        return ResponseEntity.ok( response );
    }

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex){
        return HandleValidation.handleValidationException(ex, VALIDATION_ERROR_STATUS);
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return SenderReadB2BApi.super.getRequest();
    }

    @Override
    @ExceptionHandler({PnInternalException.class})
    public ResponseEntity<NewNotificationRequestStatusResponse> getNotificationRequestStatus(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String notificationRequestId, String paProtocolNumber, String idempotenceToken) {
        String iun = new String(Base64Utils.decodeFromString(notificationRequestId), StandardCharsets.UTF_8);
        InternalNotification internalNotification = retrieveSvc.getNotificationInformation( iun, true );

        ModelMapper mapper = modelMapperFactory.createModelMapper(
                InternalNotification.class,
                NewNotificationRequestStatusResponse.class
        );
        NewNotificationRequestStatusResponse response = mapper.map(
                internalNotification,
                NewNotificationRequestStatusResponse.class
        );
        response.setNotificationRequestId( notificationRequestId );

        NotificationStatus lastStatus;
        if ( internalNotification.getNotificationStatusHistory() != null
                &&  !internalNotification.getNotificationStatusHistory().isEmpty()  ) {
            lastStatus = internalNotification.getNotificationStatusHistory().get(
                    internalNotification.getNotificationStatusHistory().size() - 1 ).getStatus();
        } else {
            log.error( "No status history for notificationRequestId={}", notificationRequestId );
            lastStatus = NotificationStatus.IN_VALIDATION;
        }

        switch ( lastStatus ) {
            case IN_VALIDATION: {
                response.setNotificationRequestStatus( "WAITING" );
                response.retryAfter( BigDecimal.valueOf(10L) );
                response.setIun( null );
                break;
            }
            case REFUSED: {
                response.setNotificationRequestStatus( "REFUSED" );
                response.setIun( null );
                break; }
            default: response.setNotificationRequestStatus( "ACCEPTED" );
        }
        return ResponseEntity.ok( response );
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentNotificationAttachment(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, BigDecimal recipientIdx, String attachmentName, List<String> xPagopaPnCxGroups) {
        NotificationAttachmentDownloadMetadataResponse response = new NotificationAttachmentDownloadMetadataResponse();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_ATCHOPEN_SND, "getSentNotificationAttachment={}", attachmentName)
                .uid(xPagopaPnUid)
                .iun(iun)
                .cxId(xPagopaPnCxId)
                .cxType(xPagopaPnCxType.toString())
                .build();
        try {
            response = notificationAttachmentService.downloadDocumentWithRedirectByIunAndRecIdxAttachName(iun, recipientIdx.intValue(), attachmentName);
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }
     
        return ResponseEntity.ok( response );
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentNotificationDocument(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, BigDecimal docIdx, List<String> xPagopaPnCxGroups) {
        NotificationAttachmentDownloadMetadataResponse response = new NotificationAttachmentDownloadMetadataResponse();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_DOCOPEN_SND, "getSentNotificationDocument={}", docIdx)
                .uid(xPagopaPnUid)
                .iun(iun)
                .cxId(xPagopaPnCxId)
                .cxType(xPagopaPnCxType.toString())
                .build();
        try {
            response = notificationAttachmentService.downloadDocumentWithRedirectByIunAndDocIndex(iun, docIdx.intValue());
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }
        return ResponseEntity.ok( response );
    }
}
