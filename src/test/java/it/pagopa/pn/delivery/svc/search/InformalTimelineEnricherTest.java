package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.*;
import it.pagopa.pn.delivery.models.InformalNotificationDetail;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.modelmapper.ModelMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InformalTimelineEnricherTest {

    private PnDeliveryPushClientImpl pnDeliveryPushClient;
    private ModelMapper modelMapper;
    private Clock clock;
    private RefinementLocalDate refinementLocalDateUtils;
    private PnDeliveryConfigs cfg;
    private InformalTimelineEnricher enricher;

    @BeforeEach
    void setup() {
        this.pnDeliveryPushClient = mock(PnDeliveryPushClientImpl.class);
        this.modelMapper = spy(new ModelMapper());
        this.clock = mock(Clock.class);
        this.refinementLocalDateUtils = spy(new RefinementLocalDate());
        this.cfg = mock(PnDeliveryConfigs.class);

        when(cfg.getInformalMaxDocumentsAvailableDays()).thenReturn("10");

        this.enricher = new InformalTimelineEnricher(pnDeliveryPushClient, modelMapper, cfg, clock, refinementLocalDateUtils);
    }

    @ParameterizedTest
    @CsvSource(
            {
                    "2026-06-15T19:00:00Z, 2026-06-15T10:00:00Z, true",
                    "2026-06-15T19:00:00Z, 2026-06-14T10:00:00Z, true",
                    "2026-06-15T19:00:00Z, 2026-06-05T10:00:00Z, true",
                    "2026-06-15T19:00:00Z, 2026-06-04T10:00:00Z, false"
            }
    )
    void shouldEnrichNotificationDetailWithNotificationAccepted(String instantTime, String acceptanceTime, boolean expectedDocumentsAvailable) {
        InternalNotification notification = buildNotification("IUN_TEST");
        notification.setDocuments(List.of(new NotificationDocument()));

        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        when(clock.instant()).thenReturn(Instant.parse(instantTime));
        InformalTimelineElementV1 timelineElement = new InformalTimelineElementV1();
        timelineElement.setCategory(InformalTimelineElementCategoryV1.REQUEST_ACCEPTED);
        timelineElement.setIngestionTimestamp(OffsetDateTime.parse(acceptanceTime));

        InformalNotificationStatusHistoryElementV1 statusHistoryElement =
                new InformalNotificationStatusHistoryElementV1();

        InformalNotificationHistoryResponse historyResponse = new InformalNotificationHistoryResponse();
        historyResponse.setTimeline(List.of(timelineElement));
        historyResponse.setInformalNotificationStatusHistory(List.of(statusHistoryElement));
        historyResponse.setInformalNotificationStatus(InformalNotificationStatusV1.ACCEPTED);

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                "IUN_TEST",
                notification.getRecipients().size(),
                notification.getSentAt()
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertNotNull(detail.getTimeline());
        assertEquals(1, detail.getTimeline().size());

        assertNotNull(detail.getNotificationStatusHistory());
        assertEquals(1, detail.getNotificationStatusHistory().size());

        assertEquals(
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusV1.ACCEPTED,
                detail.getNotificationStatus()
        );

        assertEquals(expectedDocumentsAvailable, detail.getNotification().getDocumentsAvailable());

        verify(pnDeliveryPushClient).getInformalNotificationHistory(
                "IUN_TEST",
                notification.getRecipients().size(),
                notification.getSentAt()
        );

        verify(modelMapper).map(
                timelineElement,
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalTimelineElementV1.class
        );
        verify(modelMapper).map(
                statusHistoryElement,
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusHistoryElementV1.class
        );
    }

    @Test
    void shouldEnrichNotificationDetailWithNotificationNotAccepted() {
        InternalNotification notification = buildNotification("IUN_TEST");
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T19:00:00Z"));
        InformalTimelineElementV1 timelineElement = new InformalTimelineElementV1();
        timelineElement.setCategory(InformalTimelineElementCategoryV1.REQUEST_REFUSED);
        timelineElement.setIngestionTimestamp(OffsetDateTime.parse("2026-06-15T10:00:00Z"));

        InformalNotificationStatusHistoryElementV1 statusHistoryElement =
                new InformalNotificationStatusHistoryElementV1();

        InformalNotificationHistoryResponse historyResponse = new InformalNotificationHistoryResponse();
        historyResponse.setTimeline(List.of(timelineElement));
        historyResponse.setInformalNotificationStatusHistory(List.of(statusHistoryElement));
        historyResponse.setInformalNotificationStatus(InformalNotificationStatusV1.ACCEPTED);

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                "IUN_TEST",
                notification.getRecipients().size(),
                notification.getSentAt()
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertNotNull(detail.getTimeline());
        assertEquals(1, detail.getTimeline().size());

        assertNotNull(detail.getNotificationStatusHistory());
        assertEquals(1, detail.getNotificationStatusHistory().size());

        assertEquals(
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusV1.ACCEPTED,
                detail.getNotificationStatus()
        );

        assertFalse(detail.getNotification().getDocumentsAvailable());

        verify(pnDeliveryPushClient).getInformalNotificationHistory(
                "IUN_TEST",
                notification.getRecipients().size(),
                notification.getSentAt()
        );

        verify(modelMapper).map(
                timelineElement,
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalTimelineElementV1.class
        );
        verify(modelMapper).map(
                statusHistoryElement,
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusHistoryElementV1.class
        );
    }

    @Test
    void shouldSetDocumentsAvailableFalseWhenAcceptedNotificationHasNoDocumentsAndNoPayments() {
        InternalNotification notification = buildNotification("IUN_NO_DOCS_NO_PAYMENTS_ACCEPTED");
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T19:00:00Z"));

        InformalTimelineElementV1 timelineElement = new InformalTimelineElementV1();
        timelineElement.setCategory(InformalTimelineElementCategoryV1.REQUEST_ACCEPTED);
        timelineElement.setIngestionTimestamp(OffsetDateTime.parse("2026-06-15T10:00:00Z"));

        InformalNotificationHistoryResponse historyResponse = new InformalNotificationHistoryResponse();
        historyResponse.setTimeline(List.of(timelineElement));
        historyResponse.setInformalNotificationStatusHistory(List.of(new InformalNotificationStatusHistoryElementV1()));
        historyResponse.setInformalNotificationStatus(InformalNotificationStatusV1.ACCEPTED);

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                "IUN_NO_DOCS_NO_PAYMENTS_ACCEPTED",
                notification.getRecipients().size(),
                notification.getSentAt()
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertFalse(detail.getNotification().getDocumentsAvailable());
    }

    @Test
    void shouldSetDocumentsAvailableFalseWhenNotAcceptedNotificationHasNoDocumentsAndNoPayments() {
        InternalNotification notification = buildNotification("IUN_NO_DOCS_NO_PAYMENTS_REFUSED");
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T19:00:00Z"));

        InformalTimelineElementV1 timelineElement = new InformalTimelineElementV1();
        timelineElement.setCategory(InformalTimelineElementCategoryV1.REQUEST_REFUSED);
        timelineElement.setIngestionTimestamp(OffsetDateTime.parse("2026-06-15T10:00:00Z"));

        InformalNotificationHistoryResponse historyResponse = new InformalNotificationHistoryResponse();
        historyResponse.setTimeline(List.of(timelineElement));
        historyResponse.setInformalNotificationStatusHistory(List.of(new InformalNotificationStatusHistoryElementV1()));
        historyResponse.setInformalNotificationStatus(InformalNotificationStatusV1.ACCEPTED);

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                "IUN_NO_DOCS_NO_PAYMENTS_REFUSED",
                notification.getRecipients().size(),
                notification.getSentAt()
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertFalse(detail.getNotification().getDocumentsAvailable());
    }

    @Test
    void shouldSetDocumentsAvailableTrueWhenNotificationHasDocumentsAndNoPayments() {
        InternalNotification notification = buildNotification("IUN_DOCS_NO_PAYMENTS");
        notification.setDocuments(List.of(new NotificationDocument()));

        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T19:00:00Z"));

        InformalTimelineElementV1 timelineElement = new InformalTimelineElementV1();
        timelineElement.setCategory(InformalTimelineElementCategoryV1.REQUEST_ACCEPTED);
        timelineElement.setIngestionTimestamp(OffsetDateTime.parse("2026-06-15T10:00:00Z"));

        InformalNotificationHistoryResponse historyResponse = new InformalNotificationHistoryResponse();
        historyResponse.setTimeline(List.of(timelineElement));
        historyResponse.setInformalNotificationStatusHistory(List.of(new InformalNotificationStatusHistoryElementV1()));
        historyResponse.setInformalNotificationStatus(InformalNotificationStatusV1.ACCEPTED);

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                "IUN_DOCS_NO_PAYMENTS",
                notification.getRecipients().size(),
                notification.getSentAt()
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertTrue(detail.getNotification().getDocumentsAvailable());
    }

    @Test
    void shouldSetDocumentsAvailableTrueWhenNotificationHasPaymentsAndNoDocuments() {
        InternalNotification notification = buildNotification("IUN_PAYMENTS_NO_DOCS");
        notification.getRecipients().get(0).setPayment(List.of(new NotificationPaymentInfo()));

        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(notification)
                .build();

        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T19:00:00Z"));

        InformalTimelineElementV1 timelineElement = new InformalTimelineElementV1();
        timelineElement.setCategory(InformalTimelineElementCategoryV1.REQUEST_ACCEPTED);
        timelineElement.setIngestionTimestamp(OffsetDateTime.parse("2026-06-15T10:00:00Z"));

        InformalNotificationHistoryResponse historyResponse = new InformalNotificationHistoryResponse();
        historyResponse.setTimeline(List.of(timelineElement));
        historyResponse.setInformalNotificationStatusHistory(List.of(new InformalNotificationStatusHistoryElementV1()));
        historyResponse.setInformalNotificationStatus(InformalNotificationStatusV1.ACCEPTED);

        when(pnDeliveryPushClient.getInformalNotificationHistory(
                "IUN_PAYMENTS_NO_DOCS",
                notification.getRecipients().size(),
                notification.getSentAt()
        )).thenReturn(historyResponse);

        enricher.enrichNotificationDetail(detail, false);

        assertTrue(detail.getNotification().getDocumentsAvailable());
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