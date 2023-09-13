package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.*;
import it.pagopa.pn.delivery.models.internal.notification.F24Payment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
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
    private static final String FILE_SHA_256 = "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=";

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
        Assertions.assertEquals( 1 , notificationEntity.getRecipients().get( 0 ).getPayments().size() );
        Assertions.assertEquals( NOTICE_CODE , notificationEntity.getRecipients().get( 0 ).getPayments().get( 0 ).getNoticeCode() );
        Assertions.assertEquals( CREDITOR_TAX_ID , notificationEntity.getRecipients().get( 0 ).getPayments().get( 0 ).getCreditorTaxId() );

    }

    private InternalNotification newInternalNotification() {
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setPagoPaIntMode(NewNotificationRequestV21.PagoPaIntModeEnum.NONE);
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setIun("IUN_01");
        internalNotification.setPaProtocolNumber("protocol_01");
        internalNotification.setSubject("Subject 01");
        internalNotification.setCancelledIun("IUN_05");
        internalNotification.setCancelledIun("IUN_00");
        internalNotification.setSenderPaId("PA_ID");
        internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
        internalNotification.setNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);
        internalNotification.setDocuments(List.of(NotificationDocument
                .builder()
                .digests(NotificationAttachmentDigests.builder()
                        .sha256(FILE_SHA_256)
                        .build())
                .ref(NotificationAttachmentBodyRef.builder()
                        .key("KEY")
                        .versionToken("versioneToken")
                        .build())
                .build()));
        internalNotification.setRecipients(Collections.singletonList(
                NotificationRecipient.builder()
                        .taxId("Codice Fiscale 01")
                        .denomination("Nome Cognome/Ragione Sociale")
                        .internalId( "recipientInternalId" )
                        .payments(List.of(NotificationPaymentInfo.builder()
                                .f24(F24Payment.builder()
                                        .ref(NotificationAttachmentBodyRef.builder().build())
                                        .contentType("application/json")
                                        .digests(NotificationAttachmentDigests.builder().build())
                                        .build())
                                .pagoPa(PagoPaPayment.builder()
                                        .ref(NotificationAttachmentBodyRef.builder().build())
                                        .contentType("application/json")
                                        .digests(NotificationAttachmentDigests.builder().build())
                                        .build())
                                .noticeCode("302211675775915057")
                                .noticeCodeAlternative("302351677498380984")
                                .creditorTaxId("77777777777")
                                .build())
                        )
                        .recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
                        .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                                .type( NotificationDigitalAddress.TypeEnum.PEC )
                                .address("account@dominio.it")
                                .build()).build()));
        return internalNotification;
    }
}
