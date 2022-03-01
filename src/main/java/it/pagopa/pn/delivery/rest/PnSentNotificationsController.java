package it.pagopa.pn.delivery.rest;

import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.rest.*;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.NotificationRetrieverService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController
public class PnSentNotificationsController implements
        PnDeliveryRestApi_methodGetSentNotification,
        PnDeliveryRestApi_methodGetSentNotificationDocuments,
        PnDeliveryRestApi_methodSearchSentNotification {

    private final NotificationRetrieverService retrieveSvc;
    private final PnDeliveryConfigs cfg;
    public static final String VALIDATION_ERROR_STATUS = "Validation error";

    public PnSentNotificationsController(NotificationRetrieverService retrieveSvc, PnDeliveryConfigs cfg) {
        this.retrieveSvc = retrieveSvc;
        this.cfg = cfg;
    }

    @Override
    @GetMapping(PnDeliveryRestConstants.SEND_NOTIFICATIONS_PATH)
    public ResultPaginationDto<NotificationSearchRow> searchSentNotification(
            @RequestHeader(name = PnDeliveryRestConstants.PA_ID_HEADER ) String senderId,
            @RequestParam(name = "startDate") Instant startDate,
            @RequestParam(name = "endDate") Instant endDate,
            @RequestParam(name = "recipientId", required = false) String recipientId,
            @RequestParam(name = "status", required = false) NotificationStatus status,
            @RequestParam(name = "subjectRegExp", required = false) String subjectRegExp,
            @RequestParam(name = "iunMatch", required = false) String iunMatch,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "nextPagesKey", required = false) String nextPagesKey
    ) {

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(true)
                .senderReceiverId(senderId)
                .startDate(startDate)
                .endDate(endDate)
                .filterId(recipientId)
                .status(status)
                .subjectRegExp(subjectRegExp)
                .iunMatch(iunMatch)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();
        
        return retrieveSvc.searchNotification( searchDto );
    }

    @Override
    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_SENT_PATH)
    @JsonView(value = NotificationJsonViews.Sent.class )
    public Notification getSentNotification(
            @RequestHeader(name = PnDeliveryRestConstants.PA_ID_HEADER ) String paId,
            @PathVariable( name = "iun") String iun
    ) {
        return retrieveSvc.getNotificationInformation( iun );
    }

    @Override
    @GetMapping( PnDeliveryRestConstants.NOTIFICATION_SENT_DOCUMENTS_PATH)
    public ResponseEntity<Resource> getSentNotificationDocument(
            @RequestHeader(name = PnDeliveryRestConstants.PA_ID_HEADER ) String paId,
            @PathVariable("iun") String iun,
            @PathVariable("documentIndex") int documentIndex,
            ServerHttpResponse response
    ) {
        if(cfg.isDownloadWithPresignedUrl()) {
            String redirectUrl = retrieveSvc.downloadDocumentWithRedirect(iun, documentIndex);
            response.setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
            response.getHeaders().setLocation(URI.create( redirectUrl ));
            return null;
        } else {
            ResponseEntity<Resource> resource = retrieveSvc.downloadDocument(iun, documentIndex);
            return AttachmentRestUtils.prepareAttachment( resource, iun, "doc" + documentIndex );
        }

    }

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex){
        return HandleValidation.handleValidationException(ex, VALIDATION_ERROR_STATUS);
    }

}
