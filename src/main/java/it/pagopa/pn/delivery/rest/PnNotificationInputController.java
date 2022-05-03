package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.preload.PreloadRequest;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodReceiveNotification;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.NewNotificationApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.rest.dto.ConstraintViolationImpl;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.S3PresignedUrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonView;

import it.pagopa.pn.delivery.svc.NotificationReceiverService;

import java.util.*;

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
    public ResponseEntity<NewNotificationResponse> sendNewNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, NewNotificationRequest newNotificationRequest) {
        NewNotificationResponse svcRes = svc.receiveNotification( this.convert(newNotificationRequest) );
        return ResponseEntity.ok( svcRes );
    }

    private Notification convert(NewNotificationRequest newNotificationRequest) {
        Notification notification = Notification.builder()
                .build();
        return notification;
    }

    @Override
    public ResponseEntity<List<PreLoadResponse>> presignedUploadRequest(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<PreLoadRequest> preLoadRequest) {
        //presignSvc.presignedUpload( xPagopaPnCxId, preLoadRequest );
        return null;
    }

    /*@Override
    @PostMapping(PnDeliveryRestConstants.SEND_NOTIFICATIONS_PATH )
    public NewNotificationResponse receiveNotification(
            @RequestHeader(name = PnDeliveryRestConstants.CX_ID_HEADER ) String paId,
            @RequestBody @JsonView(value = NotificationJsonViews.New.class ) Notification notification
    ) {
        if( notification.getPhysicalCommunicationType() == null ) {
            log.warn( "Add default physical communication type for paNotificationId={} from paId={}",
                    notification.getPaNotificationId(), paId );
            notification = notification.toBuilder()
                    .physicalCommunicationType(ServiceLevelType.REGISTERED_LETTER_890)
                    .build();
        }

        Notification withSender = notification.toBuilder()
                .sender( NotificationSender.builder()
                        .paId( paId )
                        .build()
                )
                .build();

        return svc.receiveNotification( withSender );
    }

    @Override
    @PostMapping( PnDeliveryRestConstants.ATTACHMENT_PRELOAD_REQUEST)
    public List<PreloadResponse> presignedUploadRequest(
            @RequestHeader(name = PnDeliveryRestConstants.CX_ID_HEADER ) String paId,
            @RequestBody List<PreloadRequest> request
    ) {
        Integer numberOfPresignedRequest = cfgs.getNumberOfPresignedRequest();
        if ( request.size() > numberOfPresignedRequest ) {
            log.error( "Presigned upload request lenght={} is more than maximum allowed={}",
                    request.size(), numberOfPresignedRequest );
            throw new PnValidationException("request",
                    Collections.singleton( new ConstraintViolationImpl<>(
                            String.format( "request.length = %d is more than maximum allowed = %d",
                                    request.size(),
                                    numberOfPresignedRequest))));
        }
        return presignSvc.presignedUpload( paId, request );
    }*/

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex){
        return HandleValidation.handleValidationException(ex,NOTIFICATION_VALIDATION_ERROR_STATUS );
    }
}
