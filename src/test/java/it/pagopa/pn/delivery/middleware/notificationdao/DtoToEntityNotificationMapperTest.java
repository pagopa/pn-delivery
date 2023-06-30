package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;


class DtoToEntityNotificationMapperTest {

    public static final String X_PAGOPA_PN_SRC_CH = "B2B";
    public static final String NOTICE_CODE = "302211675775915057";
    public static final String CREDITOR_TAX_ID = "77777777777";
    public static final String SENT_AT_DATE = "2023-03-14T15:30:23.123Z";
    public static final String NOTICE_CODE_ALTERNATIVE = "302351677498380984";
    private DtoToEntityNotificationMapper mapper;

    @BeforeEach
    void setup() {
        this.mapper = new DtoToEntityNotificationMapper();
    }
    
    @Test
    void dto2EntitySuccess() {
        InternalNotification internalNotification = newInternalNotification();
        NotificationEntity notificationEntity = mapper.dto2Entity(internalNotification);

        Assertions.assertNotNull( notificationEntity );
        Assertions.assertEquals( 2 , notificationEntity.getRecipients().get( 0 ).getPayments().size() );
        Assertions.assertEquals( NOTICE_CODE , notificationEntity.getRecipients().get( 0 ).getPayments().get( 0 ).getNoticeCode() );
        Assertions.assertEquals( NOTICE_CODE_ALTERNATIVE , notificationEntity.getRecipients().get( 0 ).getPayments().get( 1 ).getNoticeCode() );
        Assertions.assertEquals( CREDITOR_TAX_ID , notificationEntity.getRecipients().get( 0 ).getPayments().get( 0 ).getCreditorTaxId() );

    }

    private InternalNotification newInternalNotification() {
        return new InternalNotification(FullSentNotification.builder()
                .notificationFeePolicy( NotificationFeePolicy.DELIVERY_MODE )
                .pagoPaIntMode( FullSentNotification.PagoPaIntModeEnum.SYNC )
                .iun( "iun" )
                .sentAt( OffsetDateTime.parse(SENT_AT_DATE) )
                .senderPaId( "senderPaId" )
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .physicalAddress(NotificationPhysicalAddress.builder()
                                .foreignState( "Italia" )
                                .build())
                        .payment( NotificationPaymentInfo.builder()
                                .creditorTaxId( CREDITOR_TAX_ID )
                                .noticeCode( NOTICE_CODE )
                                .noticeCodeAlternative( NOTICE_CODE_ALTERNATIVE )
                                .pagoPaForm( NotificationPaymentAttachment.builder()
                                        .ref( NotificationAttachmentBodyRef.builder()
                                                .key( "KEY" )
                                                .versionToken( "VERSION_TOKEN" )
                                                .build()
                                        )
                                        .digests( NotificationAttachmentDigests.builder()
                                                .sha256( "SHA_256" )
                                                .build()
                                        )
                                        .contentType( "CONTENT_TYPE" )
                                        .build()
                                )
                                .build() )
                        .build()
                        )
                )
                .recipientIds( List.of( "recipientInternalId0" ) )
                .sourceChannel( X_PAGOPA_PN_SRC_CH )
                .build()
        );
    }
}
