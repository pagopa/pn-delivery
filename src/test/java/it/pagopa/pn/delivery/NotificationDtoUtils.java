package it.pagopa.pn.delivery;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;

import java.util.Collections;
import java.util.List;

public class NotificationDtoUtils {

    public static List<NotificationRecipient> getNotificationRecipients() {
        PhysicalAddress physicalAddress = PhysicalAddress.builder()
                .address("Via senza nome 610")
                .build();

        DigitalAddress digitalAddress = DigitalAddress.builder()
                .type(DigitalAddressType.PEC)
                .address("account@domain")
                .build();

        return Collections.singletonList(
                NotificationRecipient.builder()
                        .fc("VTIMRC00T00X000Q")
                        .digitalDomicile( digitalAddress )
                        .physicalAddress( physicalAddress )
                        .build()
        );
    }

    public static Notification buildNotification(boolean nullSender, String paId, String paNotificationId) {
        return  buildNotification( nullSender, paId, paNotificationId, getNotificationRecipients() );
    }

    public static Notification buildNotification( boolean nullSender, String paId,
                                            String paNotificationId, List<NotificationRecipient> recipients ) {

        Notification.NotificationBuilder builder = Notification.builder();
        if( ! nullSender ) {
            builder.sender( getNotificationSender( paId ) );
        }

        return builder
                .paNotificationId( paNotificationId)
                .recipients( recipients )
                .documents( Collections.emptyList() )
                .build();
    }

    public static NotificationSender getNotificationSender(String paId ) {
        return NotificationSender.builder().paId( paId ).paDenomination("paName").build();
    }
}
