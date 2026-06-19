package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.exception.PnForbiddenException;
import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV24;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.LegalNotificationDetail;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.svc.NotificationRetrieverService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRetrieverServiceTest {

    private static final String IUN = "IUN_TEST";
    private static final String SENDER_ID = "sender-pa";
    private static final String GROUP_ID = "group-id";
    private static final String GROUP_NAME = "Group Name";
    private static final String RECIPIENT_ID = "recipient-0";
    private static final String OTHER_RECIPIENT_ID = "recipient-1";
    private static final String MANDATE_ID = "mandate-1";

    private NotificationDao notificationDao;
    private PnExternalRegistriesClientImpl externalRegistriesClient;
    private PnMandateClientImpl pnMandateClient;

    private NotificationRetrieverService service;

    @BeforeEach
    void setup() {
        this.notificationDao = Mockito.mock(NotificationDao.class);
        this.externalRegistriesClient = Mockito.mock(PnExternalRegistriesClientImpl.class);
        this.pnMandateClient = Mockito.mock(PnMandateClientImpl.class);

        this.service = new NotificationRetrieverService(
                notificationDao,
                externalRegistriesClient,
                pnMandateClient
        );
    }

    @Test
    void getInternalNotificationShouldReturnNotificationWhenPresent() {
        InternalNotification notification = buildNotification();
        when(notificationDao.getNotificationByIun(IUN, true)).thenReturn(Optional.of(notification));

        InternalNotification result = service.getInternalNotification(IUN);

        Assertions.assertSame(notification, result);
    }

    @Test
    void getInternalNotificationShouldThrowWhenMissing() {
        when(notificationDao.getNotificationByIun(IUN, true)).thenReturn(Optional.empty());

        Assertions.assertThrows(PnNotificationNotFoundException.class, () -> service.getInternalNotification(IUN));
    }

    @Test
    void loadAndEnrichNotificationDetailShouldEnrichAndLabelizeGroup() {
        InternalNotification notification = buildNotification();
        notification.setGroup(GROUP_ID);
        LegalNotificationDetail detail = new LegalNotificationDetail();
        @SuppressWarnings("unchecked")
        TimelineEnricher<LegalNotificationDetail> enricher = Mockito.mock(TimelineEnricher.class);

        when(notificationDao.getNotificationByIun(IUN, true)).thenReturn(Optional.of(notification));
        when(externalRegistriesClient.getGroups(SENDER_ID, false))
                .thenReturn(List.of(new PaGroup().id(GROUP_ID).name(GROUP_NAME)));

        LegalNotificationDetail result = service.loadAndEnrichNotificationDetail(
                IUN,
                true,
                false,
                SENDER_ID,
                detail,
                enricher
        );

        Assertions.assertSame(detail, result);
        Assertions.assertSame(notification, result.getNotification());
        Assertions.assertEquals(GROUP_NAME, notification.getGroup());
        verify(enricher).enrichNotificationDetail(detail, false);
    }

    @Test
    void loadAndEnrichNotificationDetailShouldSkipEnricherWhenTimelineNotRequested() {
        InternalNotification notification = buildNotification();
        LegalNotificationDetail detail = new LegalNotificationDetail();
        @SuppressWarnings("unchecked")
        TimelineEnricher<LegalNotificationDetail> enricher = Mockito.mock(TimelineEnricher.class);

        when(notificationDao.getNotificationByIun(IUN, true)).thenReturn(Optional.of(notification));

        service.loadAndEnrichNotificationDetail(IUN, false, true, SENDER_ID, detail, enricher);

        verify(enricher, never()).enrichNotificationDetail(any(), eq(true));
    }

    @Test
    void loadCheckAndEnrichNotificationDetailShouldValidateSenderAndGroup() {
        InternalNotification notification = buildNotification();
        notification.setGroup(GROUP_ID);
        LegalNotificationDetail detail = new LegalNotificationDetail();
        @SuppressWarnings("unchecked")
        TimelineEnricher<LegalNotificationDetail> enricher = Mockito.mock(TimelineEnricher.class);

        when(notificationDao.getNotificationByIun(IUN, true)).thenReturn(Optional.of(notification));
        when(externalRegistriesClient.getGroups(SENDER_ID, false))
                .thenReturn(List.of(new PaGroup().id(GROUP_ID).name(GROUP_NAME)));

        LegalNotificationDetail result = service.loadCheckAndEnrichNotificationDetail(
                IUN,
                SENDER_ID,
                List.of(GROUP_ID),
                detail,
                enricher
        );

        Assertions.assertSame(detail, result);
        Assertions.assertEquals(GROUP_NAME, result.getNotification().getGroup());
        verify(enricher).enrichNotificationDetail(detail, true);
    }

    @Test
    void loadCheckAndEnrichNotificationDetailShouldThrowForWrongSender() {
        InternalNotification notification = buildNotification();
        LegalNotificationDetail detail = new LegalNotificationDetail();
        @SuppressWarnings("unchecked")
        TimelineEnricher<LegalNotificationDetail> enricher = Mockito.mock(TimelineEnricher.class);

        when(notificationDao.getNotificationByIun(IUN, true)).thenReturn(Optional.of(notification));

        Assertions.assertThrows(
                PnNotificationNotFoundException.class,
                () -> service.loadCheckAndEnrichNotificationDetail(IUN, "wrong-sender", List.of(GROUP_ID), detail, enricher)
        );
    }

    @Test
    void resolveIunFromRequestIdShouldDecodeBase64RequestId() {
        String encodedIun = Base64.getEncoder().encodeToString(IUN.getBytes());
        when(notificationDao.getRequestId(SENDER_ID, "protocol", "idem")).thenReturn(Optional.of(encodedIun));

        String result = service.resolveIunFromRequestId(SENDER_ID, "protocol", "idem");

        Assertions.assertEquals(IUN, result);
    }

    @Test
    void resolveIunFromRequestIdShouldThrowWhenRequestIdMissing() {
        when(notificationDao.getRequestId(SENDER_ID, "protocol", "idem")).thenReturn(Optional.empty());

        Assertions.assertThrows(
                PnNotificationNotFoundException.class,
                () -> service.resolveIunFromRequestId(SENDER_ID, "protocol", "idem")
        );
    }

    @Test
    void filterRecipientsShouldMaskOtherRecipientsForNonPa() {
        InternalNotification notification = buildNotification();
        InternalAuthHeader authHeader = new InternalAuthHeader("PF", RECIPIENT_ID, "uid", null);

        NotificationRetrieverService.filterRecipients(notification, authHeader, 0);

        Assertions.assertEquals(2, notification.getRecipients().size());
        Assertions.assertNotNull(notification.getRecipients().get(0).getTaxId());
        Assertions.assertNull(notification.getRecipients().get(1).getTaxId());
        Assertions.assertNull(notification.getRecipients().get(1).getDenomination());
        Assertions.assertNull(notification.getRecipients().get(1).getDigitalDomicile());
        Assertions.assertNull(notification.getRecipients().get(1).getPhysicalAddress());
    }

    @Test
    void filterRecipientsShouldNotMaskRecipientsForPa() {
        InternalNotification notification = buildNotification();
        InternalAuthHeader authHeader = new InternalAuthHeader("PA", RECIPIENT_ID, "uid", null);

        NotificationRetrieverService.filterRecipients(notification, authHeader, 0);

        Assertions.assertNotNull(notification.getRecipients().get(1).getTaxId());
        Assertions.assertNotNull(notification.getRecipients().get(1).getDenomination());
    }

    @Test
    void checkIUNAndInternalIdShouldPassForRecipient() {
        InternalNotification notification = buildNotification();
        when(notificationDao.getNotificationByIun(IUN, true)).thenReturn(Optional.of(notification));

        Assertions.assertDoesNotThrow(() -> service.checkIUNAndInternalId(IUN, RECIPIENT_ID, null, null, null));
    }

    @Test
    void checkIUNAndInternalIdShouldThrowForbiddenForNonRecipient() {
        InternalNotification notification = buildNotification();
        when(notificationDao.getNotificationByIun(IUN, true)).thenReturn(Optional.of(notification));

        Assertions.assertThrows(
                PnForbiddenException.class,
                () -> service.checkIUNAndInternalId(IUN, "missing-recipient", null, null, null)
        );
    }

    @Test
    void checkIUNAndInternalIdShouldValidateMandateWhenPresent() {
        InternalNotification notification = buildNotification();
        InternalMandateDto mandate = new InternalMandateDto();
        mandate.setMandateId(MANDATE_ID);
        mandate.setDelegate(RECIPIENT_ID);
        mandate.setDelegator("delegator-1");

        when(notificationDao.getNotificationByIun(IUN, true)).thenReturn(Optional.of(notification));
        when(externalRegistriesClient.getRootSenderId(SENDER_ID)).thenReturn("root-sender");
        when(pnMandateClient.listMandatesByDelegateV2(
                RECIPIENT_ID,
                MANDATE_ID,
                CxTypeAuthFleet.PF,
                List.of("group-1"),
                notification.getSentAt(),
                IUN,
                "root-sender"
        )).thenReturn(List.of(mandate));

        Assertions.assertDoesNotThrow(() -> service.checkIUNAndInternalId(IUN, RECIPIENT_ID, MANDATE_ID, "PF", List.of("group-1")));
    }

    @Test
    void checkIUNAndInternalIdShouldThrowWhenMandateIsMissing() {
        InternalNotification notification = buildNotification();

        when(notificationDao.getNotificationByIun(IUN, true)).thenReturn(Optional.of(notification));
        when(externalRegistriesClient.getRootSenderId(SENDER_ID)).thenReturn("root-sender");
        when(pnMandateClient.listMandatesByDelegateV2(
                RECIPIENT_ID,
                MANDATE_ID,
                null,
                null,
                notification.getSentAt(),
                IUN,
                "root-sender"
        )).thenReturn(List.of());

        Assertions.assertThrows(
                PnMandateNotFoundException.class,
                () -> service.checkIUNAndInternalId(IUN, RECIPIENT_ID, MANDATE_ID, null, null)
        );
    }

    private InternalNotification buildNotification() {
        InternalNotification notification = new InternalNotification();
        notification.setIun(IUN);
        notification.setSenderPaId(SENDER_ID);
        notification.setSentAt(OffsetDateTime.parse("2026-06-18T10:15:30Z"));
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
                .build();
    }
}
