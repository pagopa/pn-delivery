package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.NewNotificationApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.rest.dto.ConstraintViolationImpl;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.NotificationReceiverService;
import it.pagopa.pn.delivery.svc.S3PresignedUrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class PnNotificationInputController implements NewNotificationApi {

    public static final String NOTIFICATION_VALIDATION_ERROR_STATUS = "Notification validation error";
    private final PnDeliveryConfigs cfgs;
    private final NotificationReceiverService svc;
    private final S3PresignedUrlService presignSvc;

    public PnNotificationInputController(PnDeliveryConfigs cfgs, NotificationReceiverService svc, S3PresignedUrlService presignSvc) {
        this.cfgs = cfgs;
        this.svc = svc;
        this.presignSvc = presignSvc;
    }

    @Override
    public ResponseEntity<NewNotificationResponse> sendNewNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, NewNotificationRequest newNotificationRequest, List<String> xPagopaPnCxGroups) {
        NewNotificationResponse svcRes = svc.receiveNotification(xPagopaPnCxId, newNotificationRequest);
        return ResponseEntity.ok( svcRes );
    }


    @Override
    public ResponseEntity<List<PreLoadResponse>> presignedUploadRequest(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<PreLoadRequest> preLoadRequest) {
        Integer numberOfPresignedRequest = cfgs.getNumberOfPresignedRequest();
        if ( preLoadRequest.size() > numberOfPresignedRequest ) {
            log.error( "Presigned upload request lenght={} is more than maximum allowed={}",
                    preLoadRequest.size(), numberOfPresignedRequest );
            throw new PnValidationException("request",
                    Collections.singleton( new ConstraintViolationImpl<>(
                            String.format( "request.length = %d is more than maximum allowed = %d",
                                    preLoadRequest.size(),
                                    numberOfPresignedRequest))));
        }
        List<PreLoadResponse> preLoadResponses = presignSvc.presignedUpload( xPagopaPnCxId, preLoadRequest );
        return ResponseEntity.ok( preLoadResponses );
    }

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex){
        return HandleValidation.handleValidationException(ex,NOTIFICATION_VALIDATION_ERROR_STATUS );
    }
}
