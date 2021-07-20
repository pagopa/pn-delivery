package it.pagopa.pn.delivery.model.notification.timeline;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public class TimelineElement {

    public enum EventCategory {
        RECEIVED_ACK,
        NOTIFICATION_PATH_CHOOSE,
        SEND_DIGITAL_DOMICILE,
        SEND_DIGITAL_DOMICILE_FEEDBACK,
    }

    private Instant timestamp;
    private EventCategory eventCategory;

    @Schema(oneOf = {
            NotificationPathChooseDetails.class, ReceivedDetails.class,
            SendDigitalDetails.class, SendDigitalFeedbackDetails.class
        })
    private TimelineElementDetails details;

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public EventCategory getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(EventCategory eventCategory) {
        this.eventCategory = eventCategory;
    }

    public TimelineElementDetails getDetails() {
        return details;
    }

    public void setDetails(TimelineElementDetails details) {
        this.details = details;
    }
}
