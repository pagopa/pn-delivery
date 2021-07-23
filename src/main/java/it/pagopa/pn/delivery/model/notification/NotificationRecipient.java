package it.pagopa.pn.delivery.model.notification;


import it.pagopa.pn.delivery.model.notification.address.DigitalAddress;
import it.pagopa.pn.delivery.model.notification.address.PhysicalAddress;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@UserDefinedType
public class NotificationRecipient {

    private String fc;
    private DigitalAddress digitalDomicile;
    private PhysicalAddress physicalAddress;

}
