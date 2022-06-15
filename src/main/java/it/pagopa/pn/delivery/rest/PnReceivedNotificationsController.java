package it.pagopa.pn.delivery.rest;
;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.RecipientReadApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleNotFound;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
public class PnReceivedNotificationsController implements RecipientReadApi {
    private final NotificationRetrieverService retrieveSvc;
    private final NotificationAttachmentService notificationAttachmentService;

    private final ModelMapperFactory modelMapperFactory;
    public static final String VALIDATION_ERROR_STATUS = "Validation error";
    public static final String NOT_FOUND_ERROR_STATUS = "Not Found Error";

    public PnReceivedNotificationsController(NotificationRetrieverService retrieveSvc, NotificationAttachmentService notificationAttachmentService, ModelMapperFactory modelMapperFactory) {
        this.retrieveSvc = retrieveSvc;
        this.notificationAttachmentService = notificationAttachmentService;
        this.modelMapperFactory = modelMapperFactory;
    }

    @Override
    public ResponseEntity<NotificationSearchResponse> searchReceivedNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, Date startDate, Date endDate, List<String> xPagopaPnCxGroups, String mandateId, String senderId, NotificationStatus status, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey) {
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(false)
                .senderReceiverId(xPagopaPnCxId)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .mandateId(mandateId)
                .filterId(senderId)
                .status(status)
                //.groups( groups != null ? Arrays.asList( groups ) : null )
                .subjectRegExp(subjectRegExp)
                .iunMatch(iunMatch)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();

        ResultPaginationDto<NotificationSearchRow, String> serviceResult = retrieveSvc.searchNotification(searchDto);

        ModelMapper mapper = modelMapperFactory.createModelMapper(ResultPaginationDto.class, NotificationSearchResponse.class);
        NotificationSearchResponse response = mapper.map(serviceResult, NotificationSearchResponse.class);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<FullReceivedNotification> getReceivedNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, List<String> xPagopaPnCxGroups) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        FullReceivedNotification result = null;
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VIEW, "getReceivedNotification")
                .cxId(xPagopaPnCxId)
                .cxType(xPagopaPnCxType.toString())
                .iun(iun)
                .uid(xPagopaPnUid)
                .build();
        try {
            InternalNotification internalNotification = retrieveSvc.getNotificationAndNotifyViewedEvent(iun, xPagopaPnCxId);

            ModelMapper mapper = modelMapperFactory.createModelMapper(InternalNotification.class, FullReceivedNotification.class);

            result = mapper.map(internalNotification, FullReceivedNotification.class);

            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedNotificationDocument(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, BigDecimal docIdx, List<String> xPagopaPnCxGroups) {
        NotificationAttachmentDownloadMetadataResponse response = notificationAttachmentService.downloadDocumentWithRedirectByIunAndDocIndex(iun, docIdx.intValue());
        return ResponseEntity.ok(response);
    }


    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedNotificationAttachment(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, String attachmentName, List<String> xPagopaPnCxGroups, String mandateId) {
        NotificationAttachmentDownloadMetadataResponse response = notificationAttachmentService.downloadDocumentWithRedirectByIunRecUidAttachNameMandateId(iun, xPagopaPnCxId, attachmentName, mandateId);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex) {
        return HandleValidation.handleValidationException(ex, VALIDATION_ERROR_STATUS);
    }

    @ExceptionHandler({PnNotFoundException.class})
    public ResponseEntity<ResErrorDto> handleNotFoundException(PnNotFoundException ex) {
        return HandleNotFound.handleNotFoundException(ex, NOT_FOUND_ERROR_STATUS);
    }
}
