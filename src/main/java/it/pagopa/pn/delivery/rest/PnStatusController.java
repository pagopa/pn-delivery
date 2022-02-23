package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.NotificationUpdateStatusDto;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.StatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PnStatusController {
    private final StatusService statusService;

    public PnStatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @PostMapping(PnDeliveryRestConstants.NOTIFICATION_UPDATE_STATUS_PATH )
    public ResponseEntity<HttpStatus> updateStatus (
            @RequestBody NotificationUpdateStatusDto dto
    ){
        log.info("Update status for iun {}", dto.getIun());
        statusService.updateStatus(dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
