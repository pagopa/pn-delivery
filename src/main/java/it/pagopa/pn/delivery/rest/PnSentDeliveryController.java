package it.pagopa.pn.delivery.rest;

import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.delivery.DeliveryService;
import it.pagopa.pn.delivery.model.notification.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/delivery/notifications/sent/")
public class PnSentDeliveryController {

    @Autowired
    private DeliveryService svc;

    @PostMapping("/")
    public Mono<ResponseEntity<Void>> send(
            @RequestBody @JsonView(value = Views.NotificationsView.Send.class ) Notification notification,
            @RequestHeader("X-PagoPA-PN-PA") String paId
    ) {
        svc.receiveNotification( paId, notification );
        return Mono.just(ResponseEntity.accepted().build());
    }


    @GetMapping("/")
    @JsonView(value = Views.NotificationsView.Sent.class )
    public Mono<ResponseEntity<Notification>> getAllSentNotification(
            @RequestHeader("X-PagoPA-PN-PA") String paId
    ) {
        return Mono.just(ResponseEntity.ok( null ));
    }


}


