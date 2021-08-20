package it.pagopa.pn.delivery;

import it.pagopa.pn.api.dto.notification.*;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NotificationFactoryForTesting {

    public static Notification buildSimpleNotification(boolean nullSender, String paId, String paNotificationId) {
        return  buildSimpleNotification( nullSender, paId, paNotificationId, getNotificationRecipients() );
    }

    public static Notification buildSimpleNotification(boolean nullSender, String paId,
                                                       String paNotificationId, List<NotificationRecipient> recipients ) {

        Notification.NotificationBuilder builder = Notification.builder();
        if( ! nullSender ) {
            builder.sender( NotificationSender.builder()
                    .paId( paId )
                    .paDenomination("paName")
                    .build()
                );
        }

        return builder
                .paNotificationId( paNotificationId)
                .recipients( recipients )
                .documents( Collections.emptyList() )
                .build();
    }

    private static List<NotificationRecipient> getNotificationRecipients() {
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


    public static Notification newNotificationWithoutPayments( boolean emptyBody ) {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Arrays.asList(
                        NotificationRecipient.builder()
                                .fc("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationAttachment.builder()
                                .savedVersionId("v01_doc00")
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .contentType( emptyBody ? null : "application/pdf")
                                .body( emptyBody ? null : "Ym9keV8wMQ==")
                                .build(),
                        NotificationAttachment.builder()
                                .savedVersionId("v01_doc01")
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .contentType( emptyBody ? null : "application/pdf")
                                .body( emptyBody ? null : "Ym9keV8wMg==")
                                .build()
                ))
                .build();
    }

    public static Notification newNotificationWithPaymentsIuvOnly( boolean emptyBody) {
        return newNotificationWithoutPayments( emptyBody ).toBuilder()
                .payment( NotificationPaymentInfo.builder()
                        .iuv( "IUV_01" )
                        .build()
                )
                .build();
    }

    public static Notification newNotificationWithPaymentsDeliveryMode( boolean emptyBody) {
        return newNotificationWithPaymentsDeliveryMode( "IUV_01", emptyBody );
    }

    public static Notification newNotificationWithPaymentsDeliveryMode( String iuv, boolean emptyBody) {
        return newNotificationWithoutPayments( emptyBody ).toBuilder()
                .payment( NotificationPaymentInfo.builder()
                        .iuv( iuv )
                        .notificationFeePolicy( NotificationPaymentInfoFeePolicies.DELIVERY_MODE )
                        .f24( NotificationPaymentInfo.F24.builder()
                                .digital( NotificationAttachment.builder()
                                        .savedVersionId("v01__F24dig")
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("sha__F24dig")
                                                .build()
                                        )
                                        .contentType( emptyBody ? null : "Content/Type")
                                        .body( emptyBody ? null : "RjI0ZGln")
                                        .build()
                                )
                                .analog( NotificationAttachment.builder()
                                        .savedVersionId("v01__F24anag")
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("sha__F24anag")
                                                .build()
                                        )
                                        .contentType( emptyBody ? null : "Content/Type")
                                        .body( emptyBody ? null : "RjI0YW5hZw==")
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build();
    }

    public static Notification newNotificationWithPaymentsFlat(boolean emptyBody) {
        return newNotificationWithoutPayments( emptyBody ).toBuilder()
                .payment( NotificationPaymentInfo.builder()
                        .iuv( "IUV_01" )
                        .notificationFeePolicy( NotificationPaymentInfoFeePolicies.FLAT_RATE )
                        .f24( NotificationPaymentInfo.F24.builder()
                                .flatRate( NotificationAttachment.builder()
                                        .savedVersionId("v01__F24flat")
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("sha__F24flat")
                                                .build()
                                        )
                                        .contentType( emptyBody ? null : "application/pdf")
                                        .body(emptyBody ? null : "RjI0ZmxhdA==")
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build();
    }
}
