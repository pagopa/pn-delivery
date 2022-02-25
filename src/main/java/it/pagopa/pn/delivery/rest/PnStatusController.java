package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.StatusService;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<ResponseUpdateStatusDto> updateStatus (
            @RequestBody RequestUpdateStatusDto requestDto
    ){
        log.info("Starting Update status for iun {}", requestDto.getIun());
        ResponseUpdateStatusDto responseDto = statusService.updateStatus(requestDto);
        return ResponseEntity.ok().body(responseDto);
    }

}
