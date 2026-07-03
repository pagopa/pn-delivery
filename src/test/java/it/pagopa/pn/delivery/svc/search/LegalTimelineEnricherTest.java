package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategoryV28;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV28;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.LegalNotificationDetail;
import it.pagopa.pn.delivery.models.internal.notification.F24Payment;
import it.pagopa.pn.delivery.models.internal.notification.MetadataAttachment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegalTimelineEnricherTest {

    private Clock clock;
    private PnDeliveryPushClientImpl pnDeliveryPushClient;
    private ModelMapper modelMapper;
    private RefinementLocalDate refinementLocalDateUtils;
    private PnDeliveryConfigs cfg;

    private LegalTimelineEnricher enricher;

    @BeforeEach
    void setup() {
        this.clock = mock(Clock.class);
        this.pnDeliveryPushClient = mock(PnDeliveryPushClientImpl.class);
        this.modelMapper = spy(new ModelMapper());
        this.refinementLocalDateUtils = spy(new RefinementLocalDate());
        this.cfg = mock(PnDeliveryConfigs.class);

        when(cfg.getMaxDocumentsAvailableDays()).thenReturn("120");
        when(cfg.getMaxFirstNoticeCodeDays()).thenReturn("5");
        when(cfg.getMaxSecondNoticeCodeDays()).thenReturn("60");

        this.enricher = new LegalTimelineEnricher(
                clock,
                pnDeliveryPushClient,
                modelMapper,
                refinementLocalDateUtils,
                cfg
        );
    }

    @Test
    void shouldReturnRefinementDateWhenTimelineContainsRefinement() {
        TimelineElementV28 refinement = new TimelineElementV28();
        refinement.setCategory(TimelineElementCategoryV28.REFINEMENT);
        refinement.setTimestamp(OffsetDateTime.parse("2026-06-10T10:15:30+02:00"));

        OffsetDateTime result = enricher.findRefinementDate(List.of(refinement), "IUN_1");

        assertNotNull(result);
        verify(refinementLocalDateUtils).setLocalRefinementDate(refinement);
    }

    @Test
    void shouldReturnRefinementDateWhenTimelineContainsNotificationViewed() {
        TimelineElementV28 viewed = new TimelineElementV28();
        viewed.setCategory(TimelineElementCategoryV28.NOTIFICATION_VIEWED);
        viewed.setTimestamp(OffsetDateTime.parse("2026-06-11T08:00:00+02:00"));

        OffsetDateTime result = enricher.findRefinementDate(List.of(viewed), "IUN_2");

        assertNotNull(result);
        verify(refinementLocalDateUtils).setLocalRefinementDate(viewed);
    }

    @Test
    void shouldReturnNullWhenTimelineDoesNotContainRefinementOrViewed() {
        TimelineElementV28 other = new TimelineElementV28();
        other.setCategory(TimelineElementCategoryV28.REQUEST_ACCEPTED);
        other.setTimestamp(OffsetDateTime.parse("2026-06-11T08:00:00+02:00"));

        OffsetDateTime result = enricher.findRefinementDate(List.of(other), "IUN_3");

        assertNull(result);
        verify(refinementLocalDateUtils, never())
                .setLocalRefinementDate(any(TimelineElementV28.class));
    }

    @Test
    void shouldUseEarliestTimelineElementBetweenRefinementAndNotificationViewed() {
        TimelineElementV28 laterViewed = new TimelineElementV28();
        laterViewed.setCategory(TimelineElementCategoryV28.NOTIFICATION_VIEWED);
        laterViewed.setTimestamp(OffsetDateTime.parse("2026-06-12T08:00:00+02:00"));

        TimelineElementV28 earlierRefinement = new TimelineElementV28();
        earlierRefinement.setCategory(TimelineElementCategoryV28.REFINEMENT);
        earlierRefinement.setTimestamp(OffsetDateTime.parse("2026-06-10T08:00:00+02:00"));

        OffsetDateTime result = enricher.findRefinementDate(
                List.of(laterViewed, earlierRefinement),
                "IUN_4"
        );

        assertNotNull(result);
        verify(refinementLocalDateUtils, times(1)).setLocalRefinementDate(earlierRefinement);
    }

    @Test
    void shouldKeepDocumentsAvailableWhenNotificationIsCancelledAndRequestBySender() {
        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T00:00:00Z"));

        InternalNotification notification = buildNotificationWithPayments("IUN_CANCELLED_BY_SENDER");

        LegalNotificationDetail detail = LegalNotificationDetail.builder()
                .notification(notification)
                .build();

        it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28 pushCancellation =
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28();
        pushCancellation.setCategory(
                it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV28.NOTIFICATION_CANCELLATION_REQUEST
        );
        pushCancellation.setTimestamp(OffsetDateTime.parse("2026-06-10T10:00:00Z"));

        NotificationHistoryResponse historyResponse = mock(NotificationHistoryResponse.class);
        when(historyResponse.getTimeline()).thenReturn(List.of(pushCancellation));
        when(historyResponse.getNotificationStatusHistory()).thenReturn(Collections.emptyList());
        when(historyResponse.getNotificationStatus()).thenReturn(
                it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED
        );

        when(pnDeliveryPushClient.getTimelineAndStatusHistory(
                eq("IUN_CANCELLED_BY_SENDER"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, true);

        assertTrue(detail.getNotification().getDocumentsAvailable());
        assertEquals(TimelineElementCategoryV28.NOTIFICATION_CANCELLATION_REQUEST,
                detail.getTimeline().get(0).getCategory());
    }

    @Test
    void shouldKeepDocumentsAvailableWhenNotificationIsNotCancelled() {
        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T00:00:00Z"));

        InternalNotification notification = buildNotificationWithPayments("IUN_NOT_CANCELLED");

        LegalNotificationDetail detail = LegalNotificationDetail.builder()
                .notification(notification)
                .build();

        it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28 pushAccepted =
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28();
        pushAccepted.setCategory(
                it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV28.REQUEST_ACCEPTED
        );
        pushAccepted.setTimestamp(OffsetDateTime.parse("2026-06-10T10:00:00Z"));

        NotificationHistoryResponse historyResponse = mock(NotificationHistoryResponse.class);
        when(historyResponse.getTimeline()).thenReturn(List.of(pushAccepted));
        when(historyResponse.getNotificationStatusHistory()).thenReturn(Collections.emptyList());
        when(historyResponse.getNotificationStatus()).thenReturn(
                it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED
        );

        when(pnDeliveryPushClient.getTimelineAndStatusHistory(
                eq("IUN_NOT_CANCELLED"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertTrue(detail.getNotification().getDocumentsAvailable());
        assertEquals(TimelineElementCategoryV28.REQUEST_ACCEPTED,
                detail.getTimeline().get(0).getCategory());
    }

    @Test
    void shouldKeepDocumentsAvailableWhenNoTimelineEventsAreReturned() {
        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T00:00:00Z"));

        InternalNotification notification = buildNotificationWithPayments("IUN_NO_REFINEMENT");

        LegalNotificationDetail detail = LegalNotificationDetail.builder()
                .notification(notification)
                .build();

        NotificationHistoryResponse historyResponse = mock(NotificationHistoryResponse.class);
        when(historyResponse.getTimeline()).thenReturn(Collections.emptyList());
        when(historyResponse.getNotificationStatusHistory()).thenReturn(Collections.emptyList());
        when(historyResponse.getNotificationStatus()).thenReturn(
                it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED
        );

        when(pnDeliveryPushClient.getTimelineAndStatusHistory(
                eq("IUN_NO_REFINEMENT"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertTrue(detail.getNotification().getDocumentsAvailable());
        assertNotNull(detail.getNotification().getDocuments());
        assertEquals(1, detail.getNotification().getDocuments().size());

        NotificationPaymentInfo paymentInfo = detail.getNotification()
                .getRecipients().get(0)
                .getPayments().get(0);

        assertNotNull(paymentInfo.getPagoPa());
        assertNotNull(paymentInfo.getPagoPa().getAttachment());
        assertNotNull(paymentInfo.getF24());
    }

    @Test
    void shouldRemoveDocumentsAndPaymentAttachmentsWhenRefinementIsExpired() {
        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T00:00:00Z"));

        InternalNotification notification = buildNotificationWithPayments("IUN_EXPIRED");

        LegalNotificationDetail detail = LegalNotificationDetail.builder()
                .notification(notification)
                .build();

        it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28 pushRefinement =
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28();
        pushRefinement.setCategory(
                it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV28.REFINEMENT
        );
        pushRefinement.setTimestamp(OffsetDateTime.parse("2026-01-01T10:00:00Z"));

        NotificationHistoryResponse historyResponse = mock(NotificationHistoryResponse.class);
        when(historyResponse.getTimeline()).thenReturn(List.of(pushRefinement));
        when(historyResponse.getNotificationStatusHistory()).thenReturn(Collections.emptyList());
        when(historyResponse.getNotificationStatus()).thenReturn(
                it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED
        );

        when(pnDeliveryPushClient.getTimelineAndStatusHistory(
                eq("IUN_EXPIRED"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertFalse(detail.getNotification().getDocumentsAvailable());
        assertNotNull(detail.getNotification().getDocuments());
        assertTrue(detail.getNotification().getDocuments().isEmpty());

        NotificationPaymentInfo paymentInfo = detail.getNotification()
                .getRecipients().get(0)
                .getPayments().get(0);

        assertNotNull(paymentInfo.getPagoPa());
        assertNull(paymentInfo.getPagoPa().getAttachment());
        assertNull(paymentInfo.getF24());
    }

    @Test
    void shouldSetDocumentsUnavailableWhenCancelledAndRequestNotBySender() {
        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T00:00:00Z"));

        InternalNotification notification = buildNotificationWithPayments("IUN_CANCELLED");

        LegalNotificationDetail detail = LegalNotificationDetail.builder()
                .notification(notification)
                .build();

        it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28 pushCancellation =
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28();
        pushCancellation.setCategory(
                it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV28.NOTIFICATION_CANCELLATION_REQUEST
        );
        pushCancellation.setTimestamp(OffsetDateTime.parse("2026-06-10T10:00:00Z"));

        NotificationHistoryResponse historyResponse = mock(NotificationHistoryResponse.class);
        when(historyResponse.getTimeline()).thenReturn(List.of(pushCancellation));
        when(historyResponse.getNotificationStatusHistory()).thenReturn(Collections.emptyList());
        when(historyResponse.getNotificationStatus()).thenReturn(
                it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED
        );

        when(pnDeliveryPushClient.getTimelineAndStatusHistory(
                eq("IUN_CANCELLED"),
                eq(notification.getRecipients().size()),
                eq(notification.getSentAt())
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertFalse(detail.getNotification().getDocumentsAvailable());
    }

    private InternalNotification buildNotificationWithPayments(String iun) {
        MetadataAttachment attachment = new MetadataAttachment();

        PagoPaPayment pagoPaPayment = new PagoPaPayment();
        pagoPaPayment.setAttachment(attachment);

        F24Payment f24Payment = new F24Payment();

        NotificationPaymentInfo paymentInfo = new NotificationPaymentInfo();
        paymentInfo.setPagoPa(pagoPaPayment);
        paymentInfo.setF24(f24Payment);

        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setPayments(List.of(paymentInfo));

        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        notification.setSentAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        notification.setRecipients(List.of(recipient));
        notification.setDocuments(new java.util.ArrayList<>(List.of(
                NotificationDocument.builder().build()
        )));

        return notification;
    }
}