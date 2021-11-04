package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodDirectAccessChallenge;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.DirectAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class PnDirectAccessController implements
        PnDeliveryRestApi_methodDirectAccessChallenge {
    private final DirectAccessService directAccessService;


    public PnDirectAccessController(DirectAccessService directAccessService) {
        this.directAccessService = directAccessService;
    }

    @Override
    @GetMapping(PnDeliveryRestConstants.DIRECT_ACCESS_PATH)
    public ResponseEntity<DirectAccessToken> directAccessChallenge(
            @RequestHeader(name = "user_secret") String userSecret,
            @RequestParam(name = "token") String token) {
        Optional<DirectAccessToken> directAccessToken = directAccessService.doChallenge( token, userSecret );
        return directAccessToken.map(accessToken -> new ResponseEntity<>(accessToken, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
    }
}
