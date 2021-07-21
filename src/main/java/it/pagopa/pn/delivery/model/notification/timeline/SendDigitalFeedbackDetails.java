package it.pagopa.pn.delivery.model.notification.timeline;

import java.util.List;

public class SendDigitalFeedbackDetails extends SendDigitalDetails {

    private List<String> errors;

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
