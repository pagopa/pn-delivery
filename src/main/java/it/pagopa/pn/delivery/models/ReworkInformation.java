package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkError;
import it.pagopa.pn.delivery.generated.openapi.server.bo.v1.dto.ReworkRequest;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;

@Data
@Builder
public class ReworkInformation {
    String reworkId;
    ReworkRequest request;
    it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkResponse response;
    String iun;
    ArrayList<ReworkError> errors;
}