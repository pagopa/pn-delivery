package it.pagopa.pn.delivery.model.errors;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ErrorResponseElement {

    private String errorCode;
    private Map<String, Object> errorParameters = new HashMap<>();

}
