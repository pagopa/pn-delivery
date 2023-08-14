package it.pagopa.pn.delivery.models;


import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true, builderMethodName = "fullSentNotificationBuilder")
@ToString
public class InternalNotification extends FullSentNotification {
    public InternalNotification(FullSentNotification fsn) {
 
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
