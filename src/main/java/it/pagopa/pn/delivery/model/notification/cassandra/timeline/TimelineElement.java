package it.pagopa.pn.delivery.model.notification.cassandra.timeline;


import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@UserDefinedType
public class TimelineElement {

    public enum EventCategory {
        RECEIVED_ACK,
        NOTIFICATION_PATH_CHOOSE,
        SEND_DIGITAL_DOMICILE,
        SEND_DIGITAL_DOMICILE_FEEDBACK,
    }

    private Instant timestamp;
    private EventCategory eventCategory;

    @Transient //campo ignorato da Cassandra
    private TimelineElementDetails details;

    // specifico per necessità di cassandra memorizzo i "details" in quattro attributi diversi
    private NotificationPathChooseDetails notificationPathChooseDetails;
    private ReceivedDetails receivedDetails;
    private SendDigitalFeedbackDetails sendDigitalFeedbackDetails;
    private SendDigitalDetails sendDigitalDetails;

    public TimelineElement(Instant timestamp, EventCategory eventCategory, TimelineElementDetails details, NotificationPathChooseDetails notificationPathChooseDetails, ReceivedDetails receivedDetails, SendDigitalFeedbackDetails sendDigitalFeedbackDetails, SendDigitalDetails sendDigitalDetails) {
        this.timestamp = timestamp;
        this.eventCategory = eventCategory;
        setDetails(details);
    }

    public void setDetails(TimelineElementDetails details) {

        if(details instanceof NotificationPathChooseDetails){
            this.notificationPathChooseDetails = (NotificationPathChooseDetails) details;
        }

        if(details instanceof ReceivedDetails){
            this.receivedDetails = (ReceivedDetails) details;
        }

        if(details instanceof SendDigitalFeedbackDetails){
            this.sendDigitalFeedbackDetails = (SendDigitalFeedbackDetails) details;
        }

        if(details instanceof SendDigitalDetails){
            this.sendDigitalDetails = (SendDigitalDetails) details;
        }
    }

    public TimelineElementDetails getDetails() {
        if(details instanceof NotificationPathChooseDetails){
            return notificationPathChooseDetails;
        }

        if(details instanceof ReceivedDetails){
           return receivedDetails;
        }

        if(details instanceof SendDigitalFeedbackDetails){
            return sendDigitalFeedbackDetails;
        }

        if(details instanceof SendDigitalDetails){
            return sendDigitalDetails;
        }
        return details;
    }
}

