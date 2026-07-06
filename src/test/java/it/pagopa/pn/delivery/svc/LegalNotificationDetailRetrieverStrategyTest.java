package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.delivery.exception.PnForbiddenException;
import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV24;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementDetailsV28;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV28;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.LegalNotificationDetail;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.svc.search.LegalTimelineEnricher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

class LegalNotificationDetailRetrieverStrategyTest {

    private static final String IUN = "IUN_TEST";
    private static final String SENDER_ID = "sender-pa";
    private static final String RECIPIENT_ID = "recipient-0";
    private static final String OTHER_RECIPIENT_ID = "recipient-1";
    private static final String DELEGATE_ID = "delegate-0";
    private static final String UID = "operator-uid";
    private static final String MANDATE_ID = "mandate-id";
    private static final String SOURCE_CHANNEL = "web";
    private static final String SOURCE_CHANNEL_DETAILS = "web-details";
    private static final List<String> GROUPS = List.of("group-1");

    private Clock clock;
    private NotificationViewedProducer notificationViewedProducer;
    private PnMandateClientImpl pnMandateClient;
    private PnExternalRegistriesClientImpl pnExternalRegistriesClient;
    private LegalTimelineEnricher legalTimelineEnricher;
    private NotificationRetrieverService notificationRetrieverService;

    private LegalNotificationDetailRetrieverStrategy strategy;

    @BeforeEach
    void setup() {
        this.clock = Mockito.mock(Clock.class);
        this.notificationViewedProducer = Mockito.mock(NotificationViewedProducer.class);
        this.pnMandateClient = Mockito.mock(PnMandateClientImpl.class);
        this.pnExternalRegistriesClient = Mockito.mock(PnExternalRegistriesClientImpl.class);
        this.legalTimelineEnricher = Mockito.mock(LegalTimelineEnricher.class);
        this.notificationRetrieverService = Mockito.mock(NotificationRetrieverService.class);

        this.strategy = new LegalNotificationDetailRetrieverStrategy(
                clock,
                notificationViewedProducer,
                pnMandateClient,
                pnExternalRegistriesClient,
                legalTimelineEnricher,
                notificationRetrieverService
        );
    }

    @Test
    void getNotificationInformationShouldDelegateToRetrieverService() {
        LegalNotificationDetail expected = LegalNotificationDetail.builder().notification(new InternalNotification()).build();

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(LegalNotificationDetail.class),
                same(legalTimelineEnricher)
        )).thenReturn(expected);

        LegalNotificationDetail result = strategy.getNotificationInformation(IUN, true, false, null);

        Assertions.assertSame(expected, result);
    }

    @Test
    void getNotificationInformationWithSenderIdCheckShouldDelegateToRetrieverService() {
        LegalNotificationDetail expected = LegalNotificationDetail.builder().notification(new InternalNotification()).build();

        when(notificationRetrieverService.loadCheckAndEnrichNotificationDetail(
                eq(IUN),
                eq(SENDER_ID),
                eq(GROUPS),
                any(LegalNotificationDetail.class),
                same(legalTimelineEnricher)
        )).thenReturn(expected);

        LegalNotificationDetail result = strategy.getNotificationInformationWithSenderIdCheck(IUN, SENDER_ID, GROUPS);

        Assertions.assertSame(expected, result);
    }

    @Test
    void getNotificationInformationByRequestIdShouldResolveIunAndDelegate() {
        LegalNotificationDetail expected = LegalNotificationDetail.builder().notification(new InternalNotification()).build();

        when(notificationRetrieverService.resolveIunFromRequestId(SENDER_ID, "protocol", "idem")).thenReturn(IUN);
        when(notificationRetrieverService.loadCheckAndEnrichNotificationDetail(
                eq(IUN),
                eq(SENDER_ID),
                eq(GROUPS),
                any(LegalNotificationDetail.class),
                same(legalTimelineEnricher)
        )).thenReturn(expected);

        LegalNotificationDetail result = strategy.getNotificationInformation(SENDER_ID, "protocol", "idem", GROUPS);

        Assertions.assertSame(expected, result);
    }

    @Test
    void getNotificationAndNotifyViewedEventShouldFilterTimelineRecipientsAndNotify() {
        LegalNotificationDetail detail = buildLegalNotificationDetail(buildNotification());
        InternalAuthHeader authHeader = new InternalAuthHeader("PF", RECIPIENT_ID, UID, null, SOURCE_CHANNEL, SOURCE_CHANNEL_DETAILS);
        PnAuditLogEvent logEvent = createAuditLog();
        Instant viewedAt = Instant.parse("2026-06-18T10:15:30Z");

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(LegalNotificationDetail.class),
                same(legalTimelineEnricher)
        )).thenReturn(detail);
        when(clock.instant()).thenReturn(viewedAt);

        LegalNotificationDetail result = strategy.getNotificationAndNotifyViewedEvent(IUN, authHeader, null, logEvent);

        Assertions.assertSame(detail, result);
        Assertions.assertEquals(2, result.getTimeline().size());
        Assertions.assertEquals("public", result.getTimeline().get(0).getElementId());
        Assertions.assertEquals("recipient-0", result.getTimeline().get(1).getElementId());
        Assertions.assertEquals(2, result.getNotification().getRecipients().size());
        Assertions.assertNotNull(result.getNotification().getRecipients().get(0).getTaxId());
        Assertions.assertNull(result.getNotification().getRecipients().get(1).getTaxId());
        Assertions.assertNull(result.getNotification().getRecipients().get(1).getDenomination());
        Assertions.assertNull(result.getNotification().getRecipients().get(1).getDigitalDomicile());
        Assertions.assertNull(result.getNotification().getRecipients().get(1).getPhysicalAddress());
        Assertions.assertNull(result.getNotification().getRecipients().get(1).getPayments());
        Assertions.assertEquals(RECIPIENT_ID, logEvent.getMdc().get(MDCUtils.MDC_PN_RECIPIENT_ID_KEY));

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
    void getNotificationAndNotifyViewedEventWithMandateShouldUseDelegatorAndPopulateDelegateInfo() {
        LegalNotificationDetail detail = buildLegalNotificationDetail(buildNotification());
        InternalAuthHeader authHeader = new InternalAuthHeader("PF", DELEGATE_ID, UID, GROUPS, SOURCE_CHANNEL, SOURCE_CHANNEL_DETAILS);
        PnAuditLogEvent logEvent = createAuditLog();
        Instant viewedAt = Instant.parse("2026-06-18T10:15:30Z");

        InternalMandateDto mandate = new InternalMandateDto();
        mandate.setMandateId(MANDATE_ID);
        mandate.setDelegator(RECIPIENT_ID);
        mandate.setDelegate(DELEGATE_ID);

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(LegalNotificationDetail.class),
                same(legalTimelineEnricher)
        )).thenReturn(detail);
        when(pnExternalRegistriesClient.getRootSenderId(SENDER_ID)).thenReturn("root-sender");
        when(pnMandateClient.listMandatesByDelegateV2(
                eq(DELEGATE_ID),
                eq(MANDATE_ID),
                eq(CxTypeAuthFleet.PF),
                eq(GROUPS),
                eq(detail.getNotification().getSentAt()),
                eq(IUN),
                eq("root-sender")
        )).thenReturn(List.of(mandate));
        when(clock.instant()).thenReturn(viewedAt);

        strategy.getNotificationAndNotifyViewedEvent(IUN, authHeader, MANDATE_ID, logEvent);

        ArgumentCaptor<NotificationViewDelegateInfo> delegateInfoCaptor = ArgumentCaptor.forClass(NotificationViewDelegateInfo.class);
        verify(notificationViewedProducer).sendNotificationViewed(
                eq(IUN),
                eq(viewedAt),
                eq(0),
                delegateInfoCaptor.capture(),
                eq(SOURCE_CHANNEL),
                eq(SOURCE_CHANNEL_DETAILS)
        );

        NotificationViewDelegateInfo delegateInfo = delegateInfoCaptor.getValue();
        Assertions.assertEquals(MANDATE_ID, delegateInfo.getMandateId());
        Assertions.assertEquals(DELEGATE_ID, delegateInfo.getInternalId());
        Assertions.assertEquals(UID, delegateInfo.getOperatorUuid());
        Assertions.assertEquals(NotificationViewDelegateInfo.DelegateType.PF, delegateInfo.getDelegateType());
        Assertions.assertEquals(RECIPIENT_ID, logEvent.getMdc().get(MDCUtils.MDC_PN_RECIPIENT_ID_KEY));
        Assertions.assertEquals(DELEGATE_ID, logEvent.getMdc().get(MDCUtils.MDC_PN_DELEGATE_ID_KEY));
        Assertions.assertEquals("STANDARD", logEvent.getMdc().get(MDCUtils.MDC_PN_MANDATE_WORKFLOW_TYPE_KEY));
    }

    @Test
    void getNotificationAndNotifyViewedEventShouldThrowForbiddenForPgWithGroups() {
        LegalNotificationDetail detail = buildLegalNotificationDetail(buildNotification());
        InternalAuthHeader authHeader = new InternalAuthHeader("PG", RECIPIENT_ID, UID, GROUPS, SOURCE_CHANNEL, SOURCE_CHANNEL_DETAILS);

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(LegalNotificationDetail.class),
                same(legalTimelineEnricher)
        )).thenReturn(detail);

        Assertions.assertThrows(
                PnForbiddenException.class,
                () -> strategy.getNotificationAndNotifyViewedEvent(IUN, authHeader, null, createAuditLog())
        );

        verifyNoInteractions(notificationViewedProducer);
    }

    @Test
    void getNotificationAndNotifyViewedEventShouldThrowWhenMandateIsMissing() {
        LegalNotificationDetail detail = buildLegalNotificationDetail(buildNotification());
        InternalAuthHeader authHeader = new InternalAuthHeader("PF", DELEGATE_ID, UID, GROUPS, SOURCE_CHANNEL, SOURCE_CHANNEL_DETAILS);

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(LegalNotificationDetail.class),
                same(legalTimelineEnricher)
        )).thenReturn(detail);
        when(pnExternalRegistriesClient.getRootSenderId(SENDER_ID)).thenReturn("root-sender");
        when(pnMandateClient.listMandatesByDelegateV2(
                eq(DELEGATE_ID),
                eq(MANDATE_ID),
                eq(CxTypeAuthFleet.PF),
                eq(GROUPS),
                eq(detail.getNotification().getSentAt()),
                eq(IUN),
                eq("root-sender")
        )).thenReturn(List.of());

        Assertions.assertThrows(
                PnMandateNotFoundException.class,
                () -> strategy.getNotificationAndNotifyViewedEvent(IUN, authHeader, MANDATE_ID, createAuditLog())
        );
    }

    @Test
    void getNotificationAndNotifyViewedEventShouldThrowWhenRecipientIsMissing() {
        LegalNotificationDetail detail = buildLegalNotificationDetail(buildNotification());
        InternalAuthHeader authHeader = new InternalAuthHeader("PF", "missing-recipient", UID, null, SOURCE_CHANNEL, SOURCE_CHANNEL_DETAILS);

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(LegalNotificationDetail.class),
                same(legalTimelineEnricher)
        )).thenReturn(detail);

        Assertions.assertThrows(
                PnNotFoundException.class,
                () -> strategy.getNotificationAndNotifyViewedEvent(IUN, authHeader, null, createAuditLog())
        );
    }

    @Test
    void checkIfNotificationIsNotCancelledShouldThrowWhenCancellationExists() {
        InternalNotification notification = buildNotification();
        LegalNotificationDetail detail = LegalNotificationDetail.builder()
                .notification(notification)
                .timeline(List.of(new TimelineElementV28().category(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategoryV28.NOTIFICATION_CANCELLATION_REQUEST)))
                .build();

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(LegalNotificationDetail.class),
                same(legalTimelineEnricher)
        )).thenReturn(detail);

        Assertions.assertThrows(PnNotificationNotFoundException.class, () -> strategy.checkIfNotificationIsNotCancelled(IUN));
    }

    @Test
    void checkIfNotificationIsNotCancelledShouldNotThrowWhenNotificationIsActive() {
        InternalNotification notification = buildNotification();
        LegalNotificationDetail detail = LegalNotificationDetail.builder()
                .notification(notification)
                .timeline(List.of(new TimelineElementV28().category(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategoryV28.REQUEST_ACCEPTED)))
                .build();

        when(notificationRetrieverService.loadAndEnrichNotificationDetail(
                eq(IUN),
                eq(true),
                eq(false),
                isNull(),
                any(LegalNotificationDetail.class),
                same(legalTimelineEnricher)
        )).thenReturn(detail);

        Assertions.assertDoesNotThrow(() -> strategy.checkIfNotificationIsNotCancelled(IUN));
    }

    private LegalNotificationDetail buildLegalNotificationDetail(InternalNotification notification) {
        TimelineElementV28 publicTimelineElement = new TimelineElementV28()
                .elementId("public");
        TimelineElementV28 currentRecipientTimelineElement = new TimelineElementV28()
                .elementId("recipient-0")
                .details(TimelineElementDetailsV28.builder().recIndex(0).build());
        TimelineElementV28 otherRecipientTimelineElement = new TimelineElementV28()
                .elementId("recipient-1")
                .details(TimelineElementDetailsV28.builder().recIndex(1).build());

        return LegalNotificationDetail.builder()
                .notification(notification)
                .timeline(new ArrayList<>(List.of(
                        publicTimelineElement,
                        currentRecipientTimelineElement,
                        otherRecipientTimelineElement
                )))
                .build();
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

    private PnAuditLogEvent createAuditLog() {
        return new PnAuditLogBuilder()
                .before(PnAuditLogEventType.AUD_NT_VIEW_DEL, "test")
                .build();
    }
}

