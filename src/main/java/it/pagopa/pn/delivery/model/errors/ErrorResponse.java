package it.pagopa.pn.delivery.model.errors;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ErrorResponse {

    private String paNotificationId;
    private String status;
    private List<ErrorResponseElement> errors = new ArrayList<>();

}
