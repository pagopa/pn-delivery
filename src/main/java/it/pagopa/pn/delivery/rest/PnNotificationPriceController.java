package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.NotificationPriceApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPriceResponse;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleNotFound;
import it.pagopa.pn.delivery.svc.NotificationPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PnNotificationPriceController implements NotificationPriceApi {

    private final NotificationPriceService service;

    public PnNotificationPriceController(NotificationPriceService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<NotificationPriceResponse> getNotificationPrice(String paTaxId, String noticeNumber) {
        log.info( "Get notification price paTaxId={} noticeNumber={}", paTaxId, noticeNumber );
        NotificationPriceResponse response = service.getNotificationPrice( paTaxId, noticeNumber );
        return ResponseEntity.ok( response );
    }

    @ExceptionHandler({PnNotFoundException.class})
    public ResponseEntity<ResErrorDto> handleNotFoundException(PnNotFoundException ex) {
        return HandleNotFound.handleNotFoundException( ex, ex.getMessage() );
    }
}
