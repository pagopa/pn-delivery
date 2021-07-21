package it.pagopa.pn.delivery.model.errors;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {

    private String paNotificationId;
    private String status;
    private List<ErrorResponseElement> errors = new ArrayList<>();

    public String getPaNotificationId() {
        return paNotificationId;
    }

    public void setPaNotificationId(String paNotificationId) {
        this.paNotificationId = paNotificationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ErrorResponseElement> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorResponseElement> errors) {
        this.errors = errors;
    }
}
