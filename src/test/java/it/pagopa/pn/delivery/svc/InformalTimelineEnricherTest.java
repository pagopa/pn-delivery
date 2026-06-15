package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalNotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalNotificationStatusHistoryElementV1;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalNotificationStatusV1;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalTimelineElementV1;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.informal.notification.InformalNotificationDetail;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InformalTimelineEnricherTest {

    private PnDeliveryPushClientImpl pnDeliveryPushClient;
    private ModelMapper modelMapper;
    private InformalTimelineEnricher enricher;

    @BeforeEach
    void setup() {
        this.pnDeliveryPushClient = mock(PnDeliveryPushClientImpl.class);
        this.modelMapper = spy(new ModelMapper());
        this.enricher = new InformalTimelineEnricher(pnDeliveryPushClient, modelMapper);
    }

    @Test
    void shouldEnrichNotificationDetailWithTimelineStatusHistoryAndStatus() {
        InternalNotification notification = buildNotification("IUN_TEST");
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        InformalTimelineElementV1 timelineElement = new InformalTimelineElementV1();
        timelineElement.setTimestamp(OffsetDateTime.parse("2026-06-15T10:00:00Z"));

        InformalNotificationStatusHistoryElementV1 statusHistoryElement =
                new InformalNotificationStatusHistoryElementV1();

        InformalNotificationHistoryResponse historyResponse = new InformalNotificationHistoryResponse();
        historyResponse.setTimeline(List.of(timelineElement));
        historyResponse.setInformalNotificationStatusHistory(List.of(statusHistoryElement));
        historyResponse.setInformalNotificationStatus(InformalNotificationStatusV1.COMPLETED);

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                eq("IUN_TEST"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertNotNull(detail.getTimeline());
        assertEquals(1, detail.getTimeline().size());

        assertNotNull(detail.getNotificationStatusHistory());
        assertEquals(1, detail.getNotificationStatusHistory().size());

        assertEquals(InformalNotificationStatusV1.COMPLETED, detail.getNotificationStatus());

        verify(pnDeliveryPushClient).getInformalNotificationHistory(
                eq("IUN_TEST"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        );

        verify(modelMapper).map(timelineElement, InformalTimelineElementV1.class);
        verify(modelMapper).map(statusHistoryElement, InformalNotificationStatusHistoryElementV1.class);
    }

    @Test
    void shouldCallProtectedEnrichmentMethodAndPopulateDetail() {
        InternalNotification notification = buildNotification("IUN_DIRECT");
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        InformalTimelineElementV1 timelineElement = new InformalTimelineElementV1();
        InformalNotificationStatusHistoryElementV1 statusHistoryElement =
                new InformalNotificationStatusHistoryElementV1();

        InformalNotificationHistoryResponse historyResponse = new InformalNotificationHistoryResponse();
        historyResponse.setTimeline(List.of(timelineElement));
        historyResponse.setInformalNotificationStatusHistory(List.of(statusHistoryElement));
        historyResponse.setInformalNotificationStatus(InformalNotificationStatusV1.COMPLETED);

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                eq("IUN_DIRECT"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        )).thenReturn(historyResponse);

        enricher.enrichInformalNotificationWithTimelineAndStatusHistory(detail);

        assertNotNull(detail.getTimeline());
        assertEquals(1, detail.getTimeline().size());
        assertNotNull(detail.getNotificationStatusHistory());
        assertEquals(1, detail.getNotificationStatusHistory().size());
        assertEquals(InformalNotificationStatusV1.COMPLETED, detail.getNotificationStatus());
    }

    @Test
    void shouldThrowNullPointerWhenTimelineIsNull() {
        InternalNotification notification = buildNotification("IUN_NULL_TIMELINE");
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        InformalNotificationHistoryResponse historyResponse = new InformalNotificationHistoryResponse();
        historyResponse.setTimeline(null);
        historyResponse.setInformalNotificationStatusHistory(List.of(new InformalNotificationStatusHistoryElementV1()));
        historyResponse.setInformalNotificationStatus(InformalNotificationStatusV1.COMPLETED);

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                eq("IUN_NULL_TIMELINE"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        )).thenReturn(historyResponse);

        assertThrows(NullPointerException.class,
                () -> enricher.enrichNotificationDetail(detail, false));
    }

    @Test
    void shouldThrowNullPointerWhenStatusHistoryIsNull() {
        InternalNotification notification = buildNotification("IUN_NULL_STATUS_HISTORY");
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        InformalNotificationHistoryResponse historyResponse = new InformalNotificationHistoryResponse();
        historyResponse.setTimeline(List.of(new InformalTimelineElementV1()));
        historyResponse.setInformalNotificationStatusHistory(null);
        historyResponse.setInformalNotificationStatus(InformalNotificationStatusV1.COMPLETED);

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                eq("IUN_NULL_STATUS_HISTORY"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        )).thenReturn(historyResponse);

        assertThrows(NullPointerException.class,
                () -> enricher.enrichNotificationDetail(detail, false));
    }

    @Test
    void shouldThrowNullPointerWhenHistoryResponseIsNull() {
        InternalNotification notification = buildNotification("IUN_NULL_RESPONSE");
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                eq("IUN_NULL_RESPONSE"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        )).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> enricher.enrichNotificationDetail(detail, false));
    }

    private InternalNotification buildNotification(String iun) {
        NotificationRecipient recipient = new NotificationRecipient();

        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        notification.setSentAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        notification.setRecipients(List.of(recipient));

        return notification;
    }
}