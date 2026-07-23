package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnForbiddenException;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategoryV28;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV28;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.NotificationSearchCommunicationType;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSearchServiceTest {

    private static final Instant BEFORE_PN_EPOCH = Instant.parse("2022-04-01T00:00:00Z");
    private static final Instant AFTER_PN_EPOCH = Instant.parse("2022-05-10T00:00:00Z");
    private static final Instant END_DATE = Instant.parse("2022-06-10T00:00:00Z");
    private static final String DELEGATE_ID = "delegate-1";
    private static final String DELEGATOR_ID = "delegator-1";
    private static final String SENDER_ID = "sender-1";
    private static final String MANDATE_ID = "mandate-1";
    private static final String FILTER_ID = "12345678901";
    private static final List<String> CX_GROUPS = List.of("group-1");

    private PnMandateClientImpl pnMandateClient;
    private PnDataVaultClientImpl dataVaultClient;
    private PnExternalRegistriesClientImpl externalRegistriesClient;
    private NotificationSearchFactory notificationSearchFactory;
    private NotificationSearch notificationSearch;
    private RefinementLocalDate refinementLocalDateUtils;

    private NotificationSearchService service;

    @BeforeEach
    void setup() {
        this.pnMandateClient = Mockito.mock(PnMandateClientImpl.class);
        this.dataVaultClient = Mockito.mock(PnDataVaultClientImpl.class);
        this.externalRegistriesClient = Mockito.mock(PnExternalRegistriesClientImpl.class);
        this.notificationSearchFactory = Mockito.mock(NotificationSearchFactory.class);
        this.notificationSearch = Mockito.mock(NotificationSearch.class);
        this.refinementLocalDateUtils = new RefinementLocalDate();

        this.service = new NotificationSearchService(
                pnMandateClient,
                dataVaultClient,
                externalRegistriesClient,
                notificationSearchFactory,
                refinementLocalDateUtils
        );
    }

    @Test
    void searchNotificationBySenderShouldLabelizeGroupsAndSerializeNextPage() {
        InputSearchNotificationDto searchDto = baseSearchDto(true).toBuilder()
                .senderReceiverId(SENDER_ID)
                .build();

        PnLastEvaluatedKey nextKey = buildLastEvaluatedKey("external-key", "pk", "value-1");
        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchResult = ResultPaginationDto.<NotificationSearchRow, PnLastEvaluatedKey>builder()
                .resultsPage(List.of(NotificationSearchRow.builder()
                        .group("group-code")
                        .iun("IUN_1")
                        .build()))
                .moreResult(true)
                .nextPagesKey(List.of(nextKey))
                .build();

        when(notificationSearchFactory.getMultiPageSearch(eq(searchDto), isNull())).thenReturn(notificationSearch);
        when(notificationSearch.searchNotificationMetadata()).thenReturn(searchResult);
        when(externalRegistriesClient.getGroups(SENDER_ID, false)).thenReturn(List.of(new PaGroup().id("group-code").name("Group Name")));

        ResultPaginationDto<NotificationSearchRow, String> result = service.searchNotification(searchDto, "PA", null);

        Assertions.assertTrue(result.isMoreResult());
        Assertions.assertEquals(1, result.getResultsPage().size());
        Assertions.assertEquals("Group Name", result.getResultsPage().get(0).getGroup());
        Assertions.assertEquals(1, result.getNextPagesKey().size());
        Assertions.assertFalse(result.getNextPagesKey().get(0).isBlank());
    }

    @Test
    void searchNotificationShouldOpaquePivaFilterIdForSender() {
        InputSearchNotificationDto searchDto = baseSearchDto(true).toBuilder()
                .filterId(FILTER_ID)
                .build();

        when(dataVaultClient.ensureRecipientByExternalId(RecipientType.PG, FILTER_ID)).thenReturn("opaque-pg");
        when(notificationSearchFactory.getMultiPageSearch(eq(searchDto), isNull())).thenReturn(notificationSearch);
        when(notificationSearch.searchNotificationMetadata()).thenReturn(emptySearchResult());

        service.searchNotification(searchDto, "PA", null);

        Assertions.assertEquals("opaque-pg", searchDto.getOpaqueFilterIdPG());
        verify(dataVaultClient).ensureRecipientByExternalId(RecipientType.PG, FILTER_ID);
    }

    @Test
    void searchNotificationShouldOpaqueFiscalCodeForSender() {
        String fiscalCode = "EEEEEEEEEEEEEEEE";
        InputSearchNotificationDto searchDto = baseSearchDto(true).toBuilder()
                .filterId(fiscalCode)
                .build();

        when(dataVaultClient.ensureRecipientByExternalId(RecipientType.PF, fiscalCode)).thenReturn("opaque-pf");
        when(dataVaultClient.ensureRecipientByExternalId(RecipientType.PG, fiscalCode)).thenReturn("opaque-pg");
        when(notificationSearchFactory.getMultiPageSearch(eq(searchDto), isNull())).thenReturn(notificationSearch);
        when(notificationSearch.searchNotificationMetadata()).thenReturn(emptySearchResult());

        service.searchNotification(searchDto, "PA", null);

        Assertions.assertEquals("opaque-pf", searchDto.getOpaqueFilterIdPF());
        Assertions.assertEquals("opaque-pg", searchDto.getOpaqueFilterIdPG());
        verify(dataVaultClient).ensureRecipientByExternalId(RecipientType.PF, fiscalCode);
        verify(dataVaultClient).ensureRecipientByExternalId(RecipientType.PG, fiscalCode);
    }

    @Test
    void searchNotificationShouldOpaqueFiscalCodeForCampaign() {
        // il filtro recipientId di una ricerca per campagna bonaria arriva in chiaro (CF) come
        // per il mittente "legale": deve essere anonimizzato allo stesso modo prima della ricerca
        String fiscalCode = "EEEEEEEEEEEEEEEE";
        InputSearchNotificationDto searchDto = baseSearchDto(false).toBuilder()
                .byCampaign(true)
                .campaignId("CAMP-TEST-01")
                .filterId(fiscalCode)
                .build();

        when(dataVaultClient.ensureRecipientByExternalId(RecipientType.PF, fiscalCode)).thenReturn("opaque-pf");
        when(dataVaultClient.ensureRecipientByExternalId(RecipientType.PG, fiscalCode)).thenReturn("opaque-pg");
        when(notificationSearchFactory.getMultiPageSearch(eq(searchDto), isNull())).thenReturn(notificationSearch);
        when(notificationSearch.searchNotificationMetadata()).thenReturn(emptySearchResult());

        service.searchNotification(searchDto, "PA", null);

        Assertions.assertEquals("opaque-pf", searchDto.getOpaqueFilterIdPF());
        Assertions.assertEquals("opaque-pg", searchDto.getOpaqueFilterIdPG());
        verify(dataVaultClient).ensureRecipientByExternalId(RecipientType.PF, fiscalCode);
        verify(dataVaultClient).ensureRecipientByExternalId(RecipientType.PG, fiscalCode);
    }

    @Test
    void searchNotificationShouldOpaquePivaFilterIdForCampaign() {
        InputSearchNotificationDto searchDto = baseSearchDto(false).toBuilder()
                .byCampaign(true)
                .campaignId("CAMP-TEST-01")
                .filterId(FILTER_ID)
                .build();

        when(dataVaultClient.ensureRecipientByExternalId(RecipientType.PG, FILTER_ID)).thenReturn("opaque-pg");
        when(notificationSearchFactory.getMultiPageSearch(eq(searchDto), isNull())).thenReturn(notificationSearch);
        when(notificationSearch.searchNotificationMetadata()).thenReturn(emptySearchResult());

        service.searchNotification(searchDto, "PA", null);

        Assertions.assertEquals("opaque-pg", searchDto.getOpaqueFilterIdPG());
        verify(dataVaultClient).ensureRecipientByExternalId(RecipientType.PG, FILTER_ID);
    }

    @Test
    void searchNotificationByReceiverWithInformalCommunicationAndMandateShouldThrowBadRequest() {
        InputSearchNotificationDto searchDto = baseSearchDto(false).toBuilder()
                .senderReceiverId(DELEGATE_ID)
                .mandateId(MANDATE_ID)
                .filterId(SENDER_ID)
                .communicationType(NotificationSearchCommunicationType.INFORMAL)
                .startDate(Instant.parse("2022-05-01T00:00:00Z"))
                .endDate(Instant.parse("2022-08-01T00:00:00Z"))
                .build();

        Assertions.assertThrows(PnBadRequestException.class,
                () -> service.searchNotification(searchDto, "PF", CX_GROUPS));

        verify(pnMandateClient, never()).listMandatesByDelegate(any(), any(), any(), any());
    }

        @Test
        void searchNotificationByReceiverWithAllCommunicationAndMandateShouldSearchLegalNotifications() {
        InputSearchNotificationDto searchDto = baseSearchDto(false).toBuilder()
            .senderReceiverId(DELEGATE_ID)
            .mandateId(MANDATE_ID)
            .communicationType(NotificationSearchCommunicationType.ALL)
            .build();

        InternalMandateDto mandate = validMandate();
        when(pnMandateClient.listMandatesByDelegate(DELEGATE_ID, MANDATE_ID, CxTypeAuthFleet.PF, CX_GROUPS))
            .thenReturn(List.of(mandate));
        when(notificationSearchFactory.getMultiPageSearch(eq(searchDto), isNull())).thenReturn(notificationSearch);
        when(notificationSearch.searchNotificationMetadata()).thenReturn(emptySearchResult());

        service.searchNotification(searchDto, "PF", CX_GROUPS);

        Assertions.assertEquals(NotificationSearchCommunicationType.LEGAL, searchDto.getCommunicationType());
        verify(notificationSearchFactory).getMultiPageSearch(eq(searchDto), isNull());
        }

        @Test
        void searchNotificationByReceiverWithLegalCommunicationAndMandateShouldKeepLegalNotifications() {
        InputSearchNotificationDto searchDto = baseSearchDto(false).toBuilder()
            .senderReceiverId(DELEGATE_ID)
            .mandateId(MANDATE_ID)
            .communicationType(NotificationSearchCommunicationType.LEGAL)
            .build();

        when(pnMandateClient.listMandatesByDelegate(DELEGATE_ID, MANDATE_ID, CxTypeAuthFleet.PF, CX_GROUPS))
            .thenReturn(List.of(validMandate()));
        when(notificationSearchFactory.getMultiPageSearch(eq(searchDto), isNull())).thenReturn(notificationSearch);
        when(notificationSearch.searchNotificationMetadata()).thenReturn(emptySearchResult());

        service.searchNotification(searchDto, "PF", CX_GROUPS);

        Assertions.assertEquals(NotificationSearchCommunicationType.LEGAL, searchDto.getCommunicationType());
        }

        @Test
        void searchNotificationByReceiverWithMandateShouldReturnMandateIdInRows() {
        InputSearchNotificationDto searchDto = baseSearchDto(false).toBuilder()
            .senderReceiverId(DELEGATE_ID)
            .mandateId(MANDATE_ID)
            .communicationType(NotificationSearchCommunicationType.LEGAL)
            .build();
        NotificationSearchRow row = NotificationSearchRow.builder().iun("IUN-1").build();
        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchResult = ResultPaginationDto.<NotificationSearchRow, PnLastEvaluatedKey>builder()
            .resultsPage(List.of(row))
            .moreResult(false)
            .nextPagesKey(Collections.emptyList())
            .build();

        when(pnMandateClient.listMandatesByDelegate(DELEGATE_ID, MANDATE_ID, CxTypeAuthFleet.PF, CX_GROUPS))
            .thenReturn(List.of(validMandate()));
        when(notificationSearchFactory.getMultiPageSearch(eq(searchDto), isNull())).thenReturn(notificationSearch);
        when(notificationSearch.searchNotificationMetadata()).thenReturn(searchResult);

        ResultPaginationDto<NotificationSearchRow, String> result = service.searchNotification(searchDto, "PF", CX_GROUPS);

        Assertions.assertEquals(MANDATE_ID, result.getResultsPage().get(0).getMandateId());
        }

    @Test
    void searchNotificationByReceiverShouldThrowForbiddenForPgWithGroupsAndNoMandate() {
        InputSearchNotificationDto searchDto = baseSearchDto(false);

        Assertions.assertThrows(PnForbiddenException.class, () -> service.searchNotification(searchDto, "PG", CX_GROUPS));

        verify(notificationSearchFactory, never()).getMultiPageSearch(any(), any());
    }

    @Test
    void searchNotificationShouldThrowMandateNotFoundWhenVisibilityDoesNotContainFilterId() {
        InputSearchNotificationDto searchDto = baseSearchDto(false).toBuilder()
                .senderReceiverId(DELEGATE_ID)
                .mandateId(MANDATE_ID)
                .filterId(SENDER_ID)
                .build();

        InternalMandateDto mandate = new InternalMandateDto();
        mandate.setMandateId(MANDATE_ID);
        mandate.setDelegate(DELEGATE_ID);
        mandate.setDelegator(DELEGATOR_ID);
        mandate.setDatefrom("2022-05-01T00:00:00Z");
        mandate.setVisibilityIds(List.of("another-sender"));

        when(pnMandateClient.listMandatesByDelegate(DELEGATE_ID, MANDATE_ID, CxTypeAuthFleet.PF, CX_GROUPS))
                .thenReturn(List.of(mandate));

        Assertions.assertThrows(PnMandateNotFoundException.class, () -> service.searchNotification(searchDto, "PF", CX_GROUPS));
    }

    @Test
    void searchNotificationShouldAdjustStartDateToPnEpoch() {
        InputSearchNotificationDto searchDto = baseSearchDto(true).toBuilder()
                .startDate(BEFORE_PN_EPOCH)
                .build();

        when(notificationSearchFactory.getMultiPageSearch(eq(searchDto), isNull())).thenReturn(notificationSearch);
        when(notificationSearch.searchNotificationMetadata()).thenReturn(emptySearchResult());

        service.searchNotification(searchDto, "PA", null);

        Assertions.assertEquals(Instant.ofEpochSecond(1651399200), searchDto.getStartDate());
    }

    @Test
    void searchNotificationShouldReturnEmptyWhenEndDateIsBeforePnEpoch() {
        InputSearchNotificationDto searchDto = baseSearchDto(true).toBuilder()
                .endDate(BEFORE_PN_EPOCH)
                .build();

        ResultPaginationDto<NotificationSearchRow, String> result = service.searchNotification(searchDto, "PA", null);

        Assertions.assertEquals(Collections.emptyList(), result.getResultsPage());
        Assertions.assertEquals(Collections.emptyList(), result.getNextPagesKey());
        Assertions.assertFalse(result.isMoreResult());
        verify(notificationSearchFactory, never()).getMultiPageSearch(any(), any());
    }

    @Test
    void searchNotificationShouldThrowInternalExceptionForInvalidNextPageKey() {
        InputSearchNotificationDto searchDto = baseSearchDto(true).toBuilder()
                .nextPagesKey("not-a-valid-key")
                .build();

        Assertions.assertThrows(PnInternalException.class, () -> service.searchNotification(searchDto, "PA", null));
    }

    @Test
    void searchNotificationShouldThrowInvalidInputWhenMandatoryFieldsAreMissing() {
        InputSearchNotificationDto invalidDto = InputSearchNotificationDto.builder()
                .startDate(AFTER_PN_EPOCH)
                .endDate(END_DATE)
                .build();

        Assertions.assertThrows(PnInvalidInputException.class, () -> service.searchNotification(invalidDto, "PA", null));
    }

    @Test
    void searchNotificationDelegatedShouldSearchAndSerializeNextPage() {
        InputSearchNotificationDelegatedDto searchDto = baseDelegatedSearchDto();
        PnLastEvaluatedKey nextKey = buildLastEvaluatedKey("external-key", "pk", "value-1");
        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchResult = ResultPaginationDto.<NotificationSearchRow, PnLastEvaluatedKey>builder()
                .resultsPage(List.of(NotificationSearchRow.builder()
                        .iun("IUN_DELEGATED")
                        .build()))
                .moreResult(true)
                .nextPagesKey(List.of(nextKey))
                .build();

        when(notificationSearchFactory.getMultiPageDelegatedSearch(eq(searchDto), isNull())).thenReturn(notificationSearch);
        when(notificationSearch.searchNotificationMetadata()).thenReturn(searchResult);

        ResultPaginationDto<NotificationSearchRow, String> result = service.searchNotificationDelegated(searchDto);

        Assertions.assertEquals(1, result.getResultsPage().size());
        Assertions.assertTrue(result.isMoreResult());
        Assertions.assertEquals(1, result.getNextPagesKey().size());
        Assertions.assertFalse(result.getNextPagesKey().get(0).isBlank());
    }

    @Test
    void searchNotificationDelegatedShouldThrowForbiddenWhenGroupIsNotAllowed() {
        InputSearchNotificationDelegatedDto searchDto = delegatedSearchDtoWithGroup("other-group");

        Assertions.assertThrows(PnForbiddenException.class, () -> service.searchNotificationDelegated(searchDto));
    }

    @Test
    void searchNotificationDelegatedShouldReturnEmptyWhenEndDateIsBeforePnEpoch() {
        InputSearchNotificationDelegatedDto searchDto = delegatedSearchDtoWithEndDate(BEFORE_PN_EPOCH);

        ResultPaginationDto<NotificationSearchRow, String> result = service.searchNotificationDelegated(searchDto);

        Assertions.assertEquals(Collections.emptyList(), result.getResultsPage());
        Assertions.assertEquals(Collections.emptyList(), result.getNextPagesKey());
        Assertions.assertFalse(result.isMoreResult());
    }

    @Test
    void searchNotificationDelegatedShouldThrowInternalExceptionForInvalidNextPageKey() {
        InputSearchNotificationDelegatedDto searchDto = delegatedSearchDtoWithNextPageKey("not-a-valid-key");

        Assertions.assertThrows(PnInternalException.class, () -> service.searchNotificationDelegated(searchDto));
    }

    @Test
    void findRefinementDateShouldUseEarliestBetweenRefinementAndViewed() {
        TimelineElementV28 refinement = TimelineElementV28.builder()
                .category(TimelineElementCategoryV28.REFINEMENT)
                .timestamp(OffsetDateTime.parse("2026-06-10T10:00:00Z"))
                .build();
        TimelineElementV28 viewed = TimelineElementV28.builder()
                .category(TimelineElementCategoryV28.NOTIFICATION_VIEWED)
                .timestamp(OffsetDateTime.parse("2026-06-09T08:00:00Z"))
                .build();

        OffsetDateTime result = service.findRefinementDate(List.of(refinement, viewed), "IUN_1");

        Assertions.assertEquals(OffsetDateTime.parse("2026-06-09T23:59:59.999999999+02:00"), result);
    }

    @Test
    void findRefinementDateShouldReturnNullWhenNoRelevantEventExists() {
        TimelineElementV28 accepted = TimelineElementV28.builder()
                .category(TimelineElementCategoryV28.REQUEST_ACCEPTED)
                .timestamp(OffsetDateTime.of(2026, 6, 10, 10, 0, 0, 0, ZoneOffset.UTC))
                .build();

        OffsetDateTime result = service.findRefinementDate(List.of(accepted), "IUN_2");

        Assertions.assertNull(result);
    }

    private InputSearchNotificationDto baseSearchDto(boolean bySender) {
        return InputSearchNotificationDto.builder()
                .senderReceiverId(DELEGATE_ID)
                .startDate(AFTER_PN_EPOCH)
                .endDate(END_DATE)
                .size(10)
                .statuses(List.of(NotificationStatusV26.ACCEPTED))
                .groups(Collections.emptyList())
                .bySender(bySender)
                .build();
    }

    private InternalMandateDto validMandate() {
        InternalMandateDto mandate = new InternalMandateDto();
        mandate.setMandateId(MANDATE_ID);
        mandate.setDelegate(DELEGATE_ID);
        mandate.setDelegator(DELEGATOR_ID);
        mandate.setDatefrom("2022-05-01T00:00:00Z");
        mandate.setDateto("2022-07-01T00:00:00Z");
        return mandate;
    }

    private InputSearchNotificationDelegatedDto baseDelegatedSearchDto() {
        return InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(AFTER_PN_EPOCH)
                .endDate(END_DATE)
                .group("group-1")
                .senderId(SENDER_ID)
                .receiverId(DELEGATOR_ID)
                .statuses(List.of(NotificationStatusV26.ACCEPTED))
                .size(10)
                .cxGroups(CX_GROUPS)
                .build();
    }

    private InputSearchNotificationDelegatedDto delegatedSearchDtoWithGroup(String group) {
        return InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(AFTER_PN_EPOCH)
                .endDate(END_DATE)
                .group(group)
                .senderId(SENDER_ID)
                .receiverId(DELEGATOR_ID)
                .statuses(List.of(NotificationStatusV26.ACCEPTED))
                .size(10)
                .cxGroups(CX_GROUPS)
                .build();
    }

    private InputSearchNotificationDelegatedDto delegatedSearchDtoWithEndDate(Instant endDate) {
        return InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(AFTER_PN_EPOCH)
                .endDate(endDate)
                .group("group-1")
                .senderId(SENDER_ID)
                .receiverId(DELEGATOR_ID)
                .statuses(List.of(NotificationStatusV26.ACCEPTED))
                .size(10)
                .cxGroups(CX_GROUPS)
                .build();
    }

    private InputSearchNotificationDelegatedDto delegatedSearchDtoWithNextPageKey(String nextPageKey) {
        return InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(AFTER_PN_EPOCH)
                .endDate(END_DATE)
                .group("group-1")
                .senderId(SENDER_ID)
                .receiverId(DELEGATOR_ID)
                .statuses(List.of(NotificationStatusV26.ACCEPTED))
                .size(10)
                .nextPageKey(nextPageKey)
                .cxGroups(CX_GROUPS)
                .build();
    }

    private ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> emptySearchResult() {
        return ResultPaginationDto.<NotificationSearchRow, PnLastEvaluatedKey>builder()
                .resultsPage(Collections.emptyList())
                .nextPagesKey(Collections.emptyList())
                .moreResult(false)
                .build();
    }

    private PnLastEvaluatedKey buildLastEvaluatedKey(String externalKey, String attributeName, String attributeValue) {
        PnLastEvaluatedKey key = new PnLastEvaluatedKey();
        key.setExternalLastEvaluatedKey(externalKey);
        key.setInternalLastEvaluatedKey(Map.of(attributeName, AttributeValue.builder().s(attributeValue).build()));
        return key;
    }
}