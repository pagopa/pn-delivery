package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachment;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ResponseUpdateStatusDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.SentNotification;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class PnInternalNotificationsController implements InternalOnlyApi {

    private final NotificationRetrieverService retrieveSvc;
    private final PnDeliveryConfigs cfg;

    public PnInternalNotificationsController(NotificationRetrieverService retrieveSvc, PnDeliveryConfigs cfg) {
        this.retrieveSvc = retrieveSvc;
        this.cfg = cfg;
    }

    @Override
    public ResponseEntity<SentNotification> getSentNotification(String iun) {
        InternalNotification notification = retrieveSvc.getNotificationInformation( iun, false );

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.createTypeMap( InternalNotification.class, SentNotification.class );
        SentNotification sentNotification = modelMapper.map(notification, SentNotification.class);
        log.info( sentNotification.toString() );
        return ResponseEntity.ok( sentNotification );
    }

    /*@GetMapping("delivery-private/notifications/{iun}")
    public Notification getSentNotification( @PathVariable( name = "iun") String iun ) {
        return retrieveSvc.getNotificationInformation( iun, false );
    }*/
}
