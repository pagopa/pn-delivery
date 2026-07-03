package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.delivery.exception.PnForbiddenException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalTimelineElementV1;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV24;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InformalNotificationDetail;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import it.pagopa.pn.delivery.svc.search.InformalTimelineEnricher;
import it.pagopa.pn.delivery.svc.search.MessageEnricher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InformalNotificationDetailRetrieverStrategyTest {

    private static final String IUN = "IUN_TEST";
    private static final String SENDER_ID = "sender-pa";
    private static final String RECIPIENT_ID = "recipient-0";
    private static final String OTHER_RECIPIENT_ID = "recipient-1";
    private static final String UID = "operator-uid";
    private static final String SOURCE_CHANNEL = "web";
    private static final String SOURCE_CHANNEL_DETAILS = "web-details";
    private static final List<String> GROUPS = List.of("group-1");

    private Clock clock;
    private NotificationViewedProducer notificationViewedProducer;
    private InformalTimelineEnricher informalTimelineEnricher;
    private NotificationRetrieverService notificationRetrieverService;
    private MessageEnricher messageEnricher;

    private InformalNotificationDetailRetrieverStrategy strategy;

    @BeforeEach
    void setup() {
        this.clock = Mockito.mock(Clock.class);
        this.notificationViewedProducer = Mockito.mock(NotificationViewedProducer.class);
        this.informalTimelineEnricher = Mockito.mock(InformalTimelineEnricher.class);
        this.notificationRetrieverService = Mockito.mock(NotificationRetrieverService.class);
        this.messageEnricher = Mockito.mock(MessageEnricher.class);

        this.strategy = new InformalNotificationDetailRetrieverStrategy(
                clock,
                notificationViewedProducer,
                informalTimelineEnricher,
                notificationRetrieverService,
                messageEnricher
        );
    }

    @Test
    void getNotificationInformationShouldDelegateToRetrieverServiceAndUseMessageEnricher() {
        InformalNotificationDetail expected = InformalNotificationDetail.builder().notification(new InternalNotification()).build();

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(InformalNotificationDetail.class),
                same(informalTimelineEnricher)
        )).thenReturn(expected);

        InformalNotificationDetail result = strategy.getNotificationInformation(IUN, true, true, false, null);

        Assertions.assertSame(expected, result);
        Mockito.verify(messageEnricher).enrichInternalNotification(expected.getNotification());
        Mockito.verify(notificationRetrieverService).loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(InformalNotificationDetail.class),
                same(informalTimelineEnricher)
        );
    }

    @Test
    void getNotificationInformationShouldDelegateToRetrieverServiceAndAvoidMessageEnricher() {
        InformalNotificationDetail expected = InformalNotificationDetail.builder().notification(new InternalNotification()).build();

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(InformalNotificationDetail.class),
                same(informalTimelineEnricher)
        )).thenReturn(expected);

        InformalNotificationDetail result = strategy.getNotificationInformation(IUN, true, false, false, null);

        Assertions.assertSame(expected, result);
        Mockito.verify(messageEnricher, Mockito.times(0)).enrichInternalNotification(expected.getNotification());
        Mockito.verify(notificationRetrieverService).loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(InformalNotificationDetail.class),
                same(informalTimelineEnricher)
        );
    }

    @Test
    void getNotificationInformationWithSenderIdCheckShouldDelegateToRetrieverService() {
        InformalNotificationDetail expected = InformalNotificationDetail.builder().notification(new InternalNotification()).build();

        when(notificationRetrieverService.loadCheckAndEnrichNotificationDetail(
                eq(IUN),
                eq(SENDER_ID),
                eq(GROUPS),
                any(InformalNotificationDetail.class),
                same(informalTimelineEnricher)
        )).thenReturn(expected);

        InformalNotificationDetail result = strategy.getNotificationInformationWithSenderIdCheck(IUN, SENDER_ID, GROUPS);

        Assertions.assertSame(expected, result);
        verifyNoInteractions(messageEnricher);
    }

    @Test
    void getNotificationInformationWithSenderIdCheckAndMessageRetrieveShouldDelegateToRetrieverService() {
        InformalNotificationDetail expected = InformalNotificationDetail.builder().notification(new InternalNotification()).build();

        when(notificationRetrieverService.loadCheckAndEnrichNotificationDetail(
                eq(IUN),
                eq(SENDER_ID),
                eq(GROUPS),
                any(InformalNotificationDetail.class),
                same(informalTimelineEnricher)
        )).thenReturn(expected);

        InformalNotificationDetail result = strategy.getNotificationInformationWithSenderIdCheck(IUN, SENDER_ID, GROUPS, true);

        Assertions.assertSame(expected, result);
        Mockito.verify(messageEnricher).enrichInternalNotification(expected.getNotification());
    }

    @Test
    void getNotificationInformationByRequestIdShouldResolveIunAndDelegate() {
        InformalNotificationDetail expected = InformalNotificationDetail.builder().notification(new InternalNotification()).build();

        when(notificationRetrieverService.resolveIunFromRequestId(SENDER_ID, "protocol", "idem")).thenReturn(IUN);
        when(notificationRetrieverService.loadCheckAndEnrichNotificationDetail(
                eq(IUN),
                eq(SENDER_ID),
                eq(GROUPS),
                any(InformalNotificationDetail.class),
                same(informalTimelineEnricher)
        )).thenReturn(expected);

        InformalNotificationDetail result = strategy.getNotificationInformation(SENDER_ID, "protocol", "idem", GROUPS);

        Assertions.assertSame(expected, result);
    }

    @Test
    void getNotificationAndNotifyViewedEventShouldFilterTimelineRecipientsAndNotify() {
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(buildNotification())
                .timeline(new ArrayList<>(List.of(
                        buildTimelineElement(null),
                        buildTimelineElement(0),
                        buildTimelineElement(1)
                )))
                .build();
        InternalAuthHeader authHeader = new InternalAuthHeader("PF", RECIPIENT_ID, UID, null, SOURCE_CHANNEL, SOURCE_CHANNEL_DETAILS);
        PnAuditLogEvent logEvent = createAuditLog();
        Instant viewedAt = Instant.parse("2026-06-18T10:15:30Z");

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(InformalNotificationDetail.class),
                same(informalTimelineEnricher)
        )).thenReturn(detail);
        when(clock.instant()).thenReturn(viewedAt);

        InformalNotificationDetail result = strategy.getNotificationAndNotifyViewedEvent(IUN, authHeader, logEvent, true);

        Assertions.assertSame(detail, result);
        Assertions.assertEquals(2, result.getTimeline().size());
        Assertions.assertEquals(2, result.getNotification().getRecipients().size());
        Assertions.assertNotNull(result.getNotification().getRecipients().get(0).getTaxId());
        Assertions.assertNull(result.getNotification().getRecipients().get(1).getTaxId());
        Assertions.assertNull(result.getNotification().getRecipients().get(1).getDenomination());
        Assertions.assertNull(result.getNotification().getRecipients().get(1).getDigitalDomicile());
        Assertions.assertNull(result.getNotification().getRecipients().get(1).getPhysicalAddress());
        Assertions.assertNull(result.getNotification().getRecipients().get(1).getPayments());
        Assertions.assertEquals(RECIPIENT_ID, logEvent.getMdc().get(MDCUtils.MDC_PN_RECIPIENT_ID_KEY));

        verify(messageEnricher).enrichInternalNotification(any());
        verify(notificationViewedProducer).sendNotificationViewed(
                IUN,
                viewedAt,
                0,
                null,
                SOURCE_CHANNEL,
                SOURCE_CHANNEL_DETAILS
        );
    }

    @Test
    void getNotificationAndNotifyViewedEventShouldThrowForbiddenForPgWithGroups() {
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(buildNotification())
                .timeline(List.of(buildTimelineElement(null)))
                .build();
        InternalAuthHeader authHeader = new InternalAuthHeader("PG", RECIPIENT_ID, UID, GROUPS, SOURCE_CHANNEL, SOURCE_CHANNEL_DETAILS);

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(InformalNotificationDetail.class),
                same(informalTimelineEnricher)
        )).thenReturn(detail);

        Assertions.assertThrows(
                PnForbiddenException.class,
                () -> strategy.getNotificationAndNotifyViewedEvent(IUN, authHeader, createAuditLog(), false)
        );

        verifyNoInteractions(notificationViewedProducer);
    }

    @Test
    void getNotificationAndNotifyViewedEventShouldThrowWhenRecipientIsMissing() {
        InformalNotificationDetail detail = InformalNotificationDetail.builder()
                .notification(buildNotification())
                .timeline(List.of(buildTimelineElement(null)))
                .build();
        InternalAuthHeader authHeader = new InternalAuthHeader("PF", "missing-recipient", UID, null, SOURCE_CHANNEL, SOURCE_CHANNEL_DETAILS);

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(InformalNotificationDetail.class),
                same(informalTimelineEnricher)
        )).thenReturn(detail);

        Assertions.assertThrows(
                PnNotFoundException.class,
                () -> strategy.getNotificationAndNotifyViewedEvent(IUN, authHeader, createAuditLog(), false)
        );
    }

    private InternalNotification buildNotification() {
        InternalNotification notification = new InternalNotification();
        notification.setIun(IUN);
        notification.setSenderPaId(SENDER_ID);
        notification.setSentAt(OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        notification.setRecipientIds(new ArrayList<>(List.of(RECIPIENT_ID, OTHER_RECIPIENT_ID)));
        notification.setRecipients(new ArrayList<>(List.of(
                buildRecipient(RECIPIENT_ID, "tax-0", "Recipient Zero"),
                buildRecipient(OTHER_RECIPIENT_ID, "tax-1", "Recipient One")
        )));
        return notification;
    }

    private NotificationRecipient buildRecipient(String internalId, String taxId, String denomination) {
        return NotificationRecipient.builder()
                .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                .internalId(internalId)
                .taxId(taxId)
                .denomination(denomination)
                .digitalDomicile(NotificationDigitalAddress.builder().address(internalId + "@pec.it").build())
                .physicalAddress(NotificationPhysicalAddress.builder().address("Via Roma 1").zip("00100").build())
                .payments(List.of(NotificationPaymentInfo.builder()
                        .pagoPa(PagoPaPayment.builder().noticeCode("302000100000019421").creditorTaxId("77777777777").build())
                        .build()))
                .build();
    }

    private InformalTimelineElementV1 buildTimelineElement(Integer recIndex) {
        InformalTimelineElementV1 timelineElement = Mockito.mock(InformalTimelineElementV1.class, Mockito.RETURNS_DEEP_STUBS);
        if (recIndex == null) {
            when(timelineElement.getDetails()).thenReturn(null);
        } else {
            when(timelineElement.getDetails().getRecIndex()).thenReturn(recIndex);
        }
        return timelineElement;
    }

    private PnAuditLogEvent createAuditLog() {
        return new PnAuditLogBuilder()
                .before(PnAuditLogEventType.AUD_NT_VIEW_DEL, "test")
                .build();
    }
}

