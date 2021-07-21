package it.pagopa.pn.delivery.model.errors;

import java.util.HashMap;
import java.util.Map;

public class ErrorResponseElement {

    private String errorCode;
    private Map<String, Object> errorParameters = new HashMap<>();

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Map<String, Object> getErrorParameters() {
        return errorParameters;
    }

    public void setErrorParameters(Map<String, Object> errorParameters) {
        this.errorParameters = errorParameters;
    }
}
