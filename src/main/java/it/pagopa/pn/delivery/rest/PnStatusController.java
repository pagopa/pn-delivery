package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ResponseUpdateStatusDto;
import it.pagopa.pn.delivery.svc.StatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

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
    public ResponseEntity<ResponseUpdateStatusDto> updateStatus(RequestUpdateStatusDto requestUpdateStatusDto) {
        log.info("Starting Update status for iun {}", requestUpdateStatusDto.getIun());
        ResponseUpdateStatusDto responseDto = statusService.updateStatus(requestUpdateStatusDto);
        return ResponseEntity.ok().body(responseDto);
    }
}
