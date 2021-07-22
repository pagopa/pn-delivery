package it.pagopa.pn.delivery.model.notification.timeline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.time.Instant;

@Data
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
/*
    @Schema(oneOf = {
            NotificationPathChooseDetails.class, ReceivedDetails.class,
            SendDigitalDetails.class, SendDigitalFeedbackDetails.class
        })
    private TimelineElementDetails details;
*/

}
