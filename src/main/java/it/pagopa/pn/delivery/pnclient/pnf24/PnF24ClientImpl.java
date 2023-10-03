package it.pagopa.pn.delivery.pnclient.pnf24;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.api.F24ControllerApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.model.F24Response;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.model.RequestAccepted;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.model.SaveF24Request;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static it.pagopa.pn.commons.log.PnLogger.EXTERNAL_SERVICES.PN_F24;

@Component
@CustomLog
@RequiredArgsConstructor
public class PnF24ClientImpl {

    private final F24ControllerApi f24ControllerApi;

    public RequestAccepted saveMetadata(String xPagopaF24CxId, String setId, SaveF24Request saveF24Request) {
        log.logInvokingExternalService(PN_F24, "saveMetadata");
        return f24ControllerApi.saveMetadata(xPagopaF24CxId, setId, saveF24Request);
    }

    public F24Response generatePDF(String xPagopaF24CxId, String setId, List<String> pathTokens, Integer cost){
        log.logInvokingExternalService(PN_F24, "generatePDF");
        return f24ControllerApi.generatePDF(xPagopaF24CxId, setId, pathTokens, cost);
    }

}
