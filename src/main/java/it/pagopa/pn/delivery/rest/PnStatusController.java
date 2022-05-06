package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.generated.openapi.server.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.svc.StatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PnStatusController implements InternalOnlyApi {
    private final StatusService statusService;

    public PnStatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    /*@PostMapping(PnDeliveryRestConstants.NOTIFICATION_UPDATE_STATUS_PATH )
    public ResponseEntity<ResponseUpdateStatusDto> updateStatus (
            @RequestBody @Valid RequestUpdateStatusDto requestDto
    ){
        log.info("Starting Update status for iun {}", requestDto.getIun());
        ResponseUpdateStatusDto responseDto = statusService.updateStatus(requestDto);
        return ResponseEntity.ok().body(responseDto);
    }*/

    @Override
    public ResponseEntity<Void> updateStatus(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestUpdateStatusDto requestUpdateStatusDto) {
        log.info("Starting Update status for iun {}", requestUpdateStatusDto.getIun());
        statusService.updateStatus(requestUpdateStatusDto);
        return ResponseEntity.ok().build();
    }

}
