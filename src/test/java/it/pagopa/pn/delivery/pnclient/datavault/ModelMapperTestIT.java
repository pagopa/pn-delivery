package it.pagopa.pn.delivery.pnclient.datavault;

import it.pagopa.pn.delivery.MockAWSObjectsTest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.LegalNotificationDetail;
import it.pagopa.pn.delivery.models.internal.notification.*;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")

class ModelMapperTestIT extends MockAWSObjectsTest {

    @Autowired
    private ModelMapper modelMapper;




    @Test
    void testmappingdetail() {
        LegalNotificationDetail legalNotificationDetail = new LegalNotificationDetail();
        legalNotificationDetail.setNotificationStatus(NotificationStatusV26.DELIVERED);
        legalNotificationDetail.setTimeline(List.of(getTimelineElement()));
        legalNotificationDetail.setNotificationStatusHistory(List.of(getNotificationStatusHistoryElement()));
        legalNotificationDetail.setNotification(getInternalNotification());
        FullSentNotificationV29 fullSentNotificationV29 = modelMapper.map(legalNotificationDetail, FullSentNotificationV29.class);
        System.out.println(fullSentNotificationV29);
    }


    public InternalNotification getInternalNotification() {
        return InternalNotification.builder()
                .iun("testIun")
                .additionalLanguages(List.of("IT"))
                .recipients(List.of(
                        NotificationRecipient.builder()
                                .taxId("testTaxId")
                                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                                .payments(List.of(
                                        NotificationPaymentInfo.builder()
                                            .pagoPa(
                                                PagoPaPayment.builder()
                                                        .creditorTaxId("testCreditorTaxId")
                                                        .noticeCode("testNoticeCode")
                                                        .attachment(MetadataAttachment.builder()
                                                                .contentType("json")
                                                                .digests(NotificationAttachmentDigests.builder()
                                                                        .sha256("testSha")
                                                                        .build()
                                                                )
                                                                .ref(NotificationAttachmentBodyRef.builder()
                                                                        .key("key")
                                                                        .build()
                                                                )
                                                                .build()
                                                        )
                                                .build()
                                            )
                                            .build()
                                ))
                                .build()
                ))
                .communicationType(CommunicationType.INFORMAL)
                .build();
    }

    public TimelineElementV28 getTimelineElement() {
        TimelineElementV28 timelineElement = new TimelineElementV28();
        timelineElement.setCategory(TimelineElementCategoryV28.NOTIFICATION_CANCELLED);
        TimelineElementDetailsV28 det = new TimelineElementDetailsV28();
        det.setAmount(100);
        timelineElement.setDetails(det);
        return timelineElement;
    }

    public NotificationStatusHistoryElementV26 getNotificationStatusHistoryElement() {
        NotificationStatusHistoryElementV26 element = new NotificationStatusHistoryElementV26();
        element.setActiveFrom(OffsetDateTime.now());
        element.setStatus(NotificationStatusV26.DELIVERED);
        return element;
    }

}