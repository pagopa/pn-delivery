package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.svc.StatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
public class PnStatusController {
    private final StatusService statusService;

    public PnStatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @PostMapping(PnDeliveryRestConstants.NOTIFICATION_UPDATE_STATUS_PATH )
    public ResponseEntity<Void> updateStatus (
            @RequestBody @Valid RequestUpdateStatusDto requestDto
    ){
        log.info("Starting Update status for iun {}", requestDto.getIun());
        statusService.updateStatus(requestDto);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<String> handleValidationException(PnValidationException ex) {
        return ResponseEntity.internalServerError().body(ex.getMessage());
    }
}
