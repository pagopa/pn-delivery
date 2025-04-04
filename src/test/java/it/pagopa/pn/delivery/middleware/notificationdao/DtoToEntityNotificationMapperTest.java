package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.NotificationLang;
import it.pagopa.pn.delivery.models.internal.notification.F24Payment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import it.pagopa.pn.delivery.models.internal.notification.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class DtoToEntityNotificationMapperTest {

    public static final String X_PAGOPA_PN_SRC_CH = "B2B";
    public static final String NOTICE_CODE = "302211675775915057";
    public static final String CREDITOR_TAX_ID = "77777777777";
    public static final Integer VAT = 22;
    private static final String FILE_SHA_256 = "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=";

    private DtoToEntityNotificationMapper mapper;

    @BeforeEach
    void setup() {
        this.mapper = new DtoToEntityNotificationMapper();
    }

    @Test
    void dto2EntitySuccessWithAdditionalLanguages() {
        InternalNotification internalNotification = newInternalNotification();

        NotificationEntity notificationEntity = mapper.dto2Entity(internalNotification);

        Assertions.assertNotNull( notificationEntity );
        Assertions.assertEquals( 1 , notificationEntity.getRecipients().get( 0 ).getPayments().size() );
        Assertions.assertEquals( NOTICE_CODE , notificationEntity.getRecipients().get( 0 ).getPayments().get( 0 ).getNoticeCode() );
        Assertions.assertEquals( CREDITOR_TAX_ID , notificationEntity.getRecipients().get( 0 ).getPayments().get( 0 ).getCreditorTaxId() );
        Assertions.assertEquals(List.of(NotificationLang.builder().lang("DE").build(),NotificationLang.builder().lang("IT").build()), notificationEntity.getLanguages());
        assertEquals( VAT, notificationEntity.getVat() );

    }

    @Test
    void dto2EntitySuccessWithITLanguages() {
        InternalNotification internalNotification = newInternalNotification();
        internalNotification.setAdditionalLanguages(null);

        NotificationEntity notificationEntity = mapper.dto2Entity(internalNotification);

        Assertions.assertNotNull( notificationEntity );
        Assertions.assertEquals( 1 , notificationEntity.getRecipients().get( 0 ).getPayments().size() );
        Assertions.assertEquals( NOTICE_CODE , notificationEntity.getRecipients().get( 0 ).getPayments().get( 0 ).getNoticeCode() );
        Assertions.assertEquals( CREDITOR_TAX_ID , notificationEntity.getRecipients().get( 0 ).getPayments().get( 0 ).getCreditorTaxId() );
        Assertions.assertEquals(List.of(NotificationLang.builder().lang("IT").build()), notificationEntity.getLanguages());
        assertEquals( VAT, notificationEntity.getVat() );
    }

    @Test
    void dto2EntitySuccessWithITLanguages2() {
        InternalNotification internalNotification = newInternalNotification();
        internalNotification.setAdditionalLanguages(Collections.emptyList());

        NotificationEntity notificationEntity = mapper.dto2Entity(internalNotification);

        Assertions.assertNotNull( notificationEntity );
        Assertions.assertEquals( 1 , notificationEntity.getRecipients().get( 0 ).getPayments().size() );
        Assertions.assertEquals( NOTICE_CODE , notificationEntity.getRecipients().get( 0 ).getPayments().get( 0 ).getNoticeCode() );
        Assertions.assertEquals( CREDITOR_TAX_ID , notificationEntity.getRecipients().get( 0 ).getPayments().get( 0 ).getCreditorTaxId() );
        Assertions.assertEquals(List.of(NotificationLang.builder().lang("IT").build()), notificationEntity.getLanguages());
        assertEquals( VAT, notificationEntity.getVat() );
    }


    @Test
    void testInternalNotification(){
        InternalNotification actualInternalNotification = new InternalNotification();
        actualInternalNotification._abstract(" abstract");
        actualInternalNotification.amount(10);
        actualInternalNotification.cancelledByIun("Cancelled By Iun");
        actualInternalNotification.cancelledIun("Cancelled Iun");
        ArrayList<NotificationDocument> documents = new ArrayList<>();
        actualInternalNotification.documents(documents);
        actualInternalNotification.documentsAvailable(true);
        actualInternalNotification.group("Group");
        actualInternalNotification.idempotenceToken("ABC123");
        actualInternalNotification.iun("Iun");
        actualInternalNotification.notificationFeePolicy(NotificationFeePolicy.FLAT_RATE);
        actualInternalNotification.notificationStatus(NotificationStatusV26.IN_VALIDATION);
        ArrayList<NotificationStatusHistoryElementV26> notificationStatusHistory = new ArrayList<>();
        actualInternalNotification.notificationStatusHistory(notificationStatusHistory);
        actualInternalNotification.paProtocolNumber("42");
        actualInternalNotification.paymentExpirationDate("2020-03-01");
        actualInternalNotification
                .physicalCommunicationType(FullSentNotificationV27.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER);
        ArrayList<String> recipientIds = new ArrayList<>();
        actualInternalNotification.recipientIds(recipientIds);
        ArrayList<NotificationRecipient> recipients = new ArrayList<>();
        actualInternalNotification.recipients(recipients);
        actualInternalNotification.senderDenomination("Sender Denomination");
        actualInternalNotification.senderPaId("42");
        actualInternalNotification.senderTaxId("42");
        actualInternalNotification.sentAt(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
        actualInternalNotification.setAbstract(" abstract");
        actualInternalNotification.setAmount(10);
        actualInternalNotification.setCancelledByIun("Cancelled By Iun");
        actualInternalNotification.setCancelledIun("Cancelled Iun");
        ArrayList<NotificationDocument> documents2 = new ArrayList<>();
        actualInternalNotification.setDocuments(documents2);
        actualInternalNotification.setDocumentsAvailable(true);
        actualInternalNotification.setGroup("Group");
        actualInternalNotification.setIdempotenceToken("ABC123");
        actualInternalNotification.setIun("Iun");
        actualInternalNotification.setNotificationFeePolicy(NotificationFeePolicy.FLAT_RATE);
        actualInternalNotification.setNotificationStatus(NotificationStatusV26.IN_VALIDATION);
        ArrayList<NotificationStatusHistoryElementV26> notificationStatusHistory2 = new ArrayList<>();
        actualInternalNotification.setNotificationStatusHistory(notificationStatusHistory2);
        actualInternalNotification.setPaProtocolNumber("42");
        actualInternalNotification.setPaymentExpirationDate("2020-03-01");
        actualInternalNotification
                .setPhysicalCommunicationType(FullSentNotificationV27.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER);
        ArrayList<String> recipientIds2 = new ArrayList<>();
        actualInternalNotification.setRecipientIds(recipientIds2);
        ArrayList<NotificationRecipient> recipients2 = new ArrayList<>();
        actualInternalNotification.setRecipients(recipients2);
        actualInternalNotification.setSenderDenomination("Sender Denomination");
        actualInternalNotification.setSenderPaId("42");
        actualInternalNotification.setSenderTaxId("42");
        OffsetDateTime sentAt = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
        actualInternalNotification.setSentAt(sentAt);
        actualInternalNotification.setSubject("Hello from the Dreaming Spires");
        actualInternalNotification.setTaxonomyCode("Taxonomy Code");
        ArrayList<TimelineElementV27> timeline = new ArrayList<>();
        actualInternalNotification.setTimeline(timeline);
        actualInternalNotification.sourceChannel("Source Channel");
        actualInternalNotification.subject("Hello from the Dreaming Spires");
        actualInternalNotification.taxonomyCode("Taxonomy Code");
        testingInternalNotification(actualInternalNotification);
    }

    private void testingInternalNotification(InternalNotification actualInternalNotification){
        ArrayList<TimelineElementV27> timeline2 = new ArrayList<>();
        actualInternalNotification.timeline(timeline2);
        assertEquals(" abstract", actualInternalNotification.getAbstract());
        assertEquals(10, actualInternalNotification.getAmount().intValue());
        assertEquals("Cancelled By Iun", actualInternalNotification.getCancelledByIun());
        assertEquals("Cancelled Iun", actualInternalNotification.getCancelledIun());
        List<TimelineElementV27> timeline3 = actualInternalNotification.getTimeline();
        assertTrue(actualInternalNotification.getDocumentsAvailable());
        assertEquals("Group", actualInternalNotification.getGroup());
        assertEquals("ABC123", actualInternalNotification.getIdempotenceToken());
        assertEquals("Iun", actualInternalNotification.getIun());
        assertEquals(NotificationFeePolicy.FLAT_RATE, actualInternalNotification.getNotificationFeePolicy());
        assertEquals(NotificationStatusV26.IN_VALIDATION, actualInternalNotification.getNotificationStatus());
        assertEquals("42", actualInternalNotification.getPaProtocolNumber());
        assertEquals("2020-03-01", actualInternalNotification.getPaymentExpirationDate());
        assertEquals(FullSentNotificationV27.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER,
                actualInternalNotification.getPhysicalCommunicationType());
        assertEquals("Sender Denomination", actualInternalNotification.getSenderDenomination());
        assertEquals("42", actualInternalNotification.getSenderPaId());
        assertEquals("42", actualInternalNotification.getSenderTaxId());
        assertEquals("Source Channel", actualInternalNotification.getSourceChannel());
        assertEquals("Hello from the Dreaming Spires", actualInternalNotification.getSubject());
        assertEquals("Taxonomy Code", actualInternalNotification.getTaxonomyCode());
        assertSame(timeline2, timeline3);
    }

    private InternalNotification newInternalNotification() {
        List<String> languages = new ArrayList<>();
        languages.add("DE");
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setPagoPaIntMode(NewNotificationRequestV25.PagoPaIntModeEnum.NONE);
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setIun("IUN_01");
        internalNotification.setPaProtocolNumber("protocol_01");
        internalNotification.setSubject("Subject 01");
        internalNotification.setCancelledIun("IUN_05");
        internalNotification.setCancelledIun("IUN_00");
        internalNotification.setSenderPaId("PA_ID");
        internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
        internalNotification.setPaFee(0);
        internalNotification.setVat(VAT);
        internalNotification.setNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);
        internalNotification.setAdditionalLanguages(languages);
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
                                        .title("title")
                                        .applyCost(false)
                                        .metadataAttachment(MetadataAttachment.builder()
                                                .ref(NotificationAttachmentBodyRef.builder().build())
                                                .contentType("application/json")
                                                .digests(NotificationAttachmentDigests.builder().build())
                                                .build())
                                        .build())
                                .pagoPa(PagoPaPayment.builder()
                                        .applyCost(false)
                                        .noticeCode("302211675775915057")
                                        .creditorTaxId("77777777777")
                                        .attachment(MetadataAttachment.builder()
                                                .ref(NotificationAttachmentBodyRef.builder().build())
                                                .contentType("application/json")
                                                .digests(NotificationAttachmentDigests.builder().build())
                                                .build())
                                        .build())
                                .build())
                        )
                        .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                        .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                                .type( NotificationDigitalAddress.TypeEnum.PEC )
                                .address("account@dominio.it")
                                .build()).build()));
        return internalNotification;
    }
}
