package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CommunicationOutcomes;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationSearchResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusV1;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.UnifiedNotificationStatus;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test del mapping bonario (WI-US4.9): converter
 * {@link NotificationSearchRow} → {@link InformalNotificationSearchRow} e fail-fast del validatore
 * {@link InformalNotificationStatusValidator} (anticipato in WI-US4.8).
 */
class ModelMapperConfigInformalSearchRowTest {

    private ModelMapper modelMapper;

    @BeforeEach
    void setup() {
        this.modelMapper = new ModelMapperConfig().modelMapper();
    }

    @Test
    void communicationTypeDefaultsToInformalWhenNull() {
        NotificationSearchRow source = baseRow(UnifiedNotificationStatus.PROCESSING)
                .toBuilder().communicationType(null).build();

        InformalNotificationSearchRow result = modelMapper.map(source, InformalNotificationSearchRow.class);

        assertEquals(InformalNotificationSearchRow.CommunicationTypeEnum.INFORMAL, result.getCommunicationType());
    }

    @Test
    void communicationTypeMappedToInformal() {
        NotificationSearchRow source = baseRow(UnifiedNotificationStatus.PROCESSING)
                .toBuilder().communicationType("INFORMAL").build();

        InformalNotificationSearchRow result = modelMapper.map(source, InformalNotificationSearchRow.class);

        assertEquals(InformalNotificationSearchRow.CommunicationTypeEnum.INFORMAL, result.getCommunicationType());
    }

    @Test
    void campaignIdIsMapped() {
        NotificationSearchRow source = baseRow(UnifiedNotificationStatus.PROCESSING)
                .toBuilder().communicationType("INFORMAL").campaignId("campaign-1").build();

        InformalNotificationSearchRow result = modelMapper.map(source, InformalNotificationSearchRow.class);

        assertEquals("campaign-1", result.getCampaignId());
    }

    @Test
    void communicationOutcomesPopulatedWhenViewedAndDeliveredPresent() {
        NotificationSearchRow source = baseRow(UnifiedNotificationStatus.PROCESSING).toBuilder()
                .communicationType("INFORMAL")
                .viewed(true)
                .delivered(false)
                .build();

        InformalNotificationSearchRow result = modelMapper.map(source, InformalNotificationSearchRow.class);

        CommunicationOutcomes outcomes = result.getCommunicationOutcomes();
        assertNotNull(outcomes);
        assertEquals(Boolean.TRUE, outcomes.getViewed());
        assertEquals(Boolean.FALSE, outcomes.getDelivered());
    }

    @Test
    void communicationOutcomesNullWhenNoEsitoPresent() {
        NotificationSearchRow source = baseRow(UnifiedNotificationStatus.PROCESSING).toBuilder()
                .communicationType("INFORMAL")
                .viewed(null)
                .delivered(null)
                .build();

        InformalNotificationSearchRow result = modelMapper.map(source, InformalNotificationSearchRow.class);

        assertNull(result.getCommunicationOutcomes());
    }

    @Test
    void desiredFeedbackIsNotExposedOnOutput() {
        // il DTO bonario non espone desiredFeedback: garanzia a compile-time + nessun metodo a runtime
        assertTrue(java.util.Arrays.stream(InformalNotificationSearchRow.class.getMethods())
                .noneMatch(m -> m.getName().toLowerCase().contains("desiredfeedback")));
    }

    @Test
    void paginationWrapperIsMappedToResponse() {
        NotificationSearchRow row = baseRow(UnifiedNotificationStatus.PROCESSING).toBuilder()
                .communicationType("INFORMAL")
                .campaignId("campaign-1")
                .viewed(true)
                .delivered(false)
                .build();

        ResultPaginationDto<NotificationSearchRow, String> page = new ResultPaginationDto<>();
        page.setResultsPage(List.of(row));
        page.setMoreResult(false);
        page.setNextPagesKey(List.of());

        InformalNotificationSearchResponse response =
                modelMapper.map(page, InformalNotificationSearchResponse.class);

        assertNotNull(response.getResultsPage());
        assertEquals(1, response.getResultsPage().size());
        InformalNotificationSearchRow mapped = response.getResultsPage().get(0);
        assertEquals(InformalNotificationSearchRow.CommunicationTypeEnum.INFORMAL, mapped.getCommunicationType());
        assertEquals("campaign-1", mapped.getCampaignId());
        assertEquals(InformalNotificationStatusV1.PROCESSING, mapped.getNotificationStatus());
        assertNotNull(mapped.getCommunicationOutcomes());
        assertEquals(Boolean.TRUE, mapped.getCommunicationOutcomes().getViewed());
        assertEquals(Boolean.FALSE, mapped.getCommunicationOutcomes().getDelivered());
    }

    // --- Validatore InformalNotificationStatusValidator -------------------------------------------

    @Test
    void informalStatusPassesValidation() {
        assertDoesNotThrow(() -> InformalNotificationStatusValidator
                .assertInformalCompatible(page(baseRow(UnifiedNotificationStatus.COMPLETED_REACHED))));
    }

    @Test
    void sharedRefusedStatusPassesValidation() {
        assertDoesNotThrow(() -> InformalNotificationStatusValidator
                .assertInformalCompatible(page(baseRow(UnifiedNotificationStatus.REFUSED))));
    }

    @Test
    void nullStatusIsTolerated() {
        assertDoesNotThrow(() -> InformalNotificationStatusValidator
                .assertInformalCompatible(page(baseRow(null))));
    }

    @Test
    void nullServiceResultIsTolerated() {
        assertDoesNotThrow(() -> InformalNotificationStatusValidator.assertInformalCompatible(null));
    }

    @Test
    void emptyResultsPageIsTolerated() {
        ResultPaginationDto<NotificationSearchRow, String> empty = ResultPaginationDto
                .<NotificationSearchRow, String>builder()
                .resultsPage(new ArrayList<>())
                .build();

        assertDoesNotThrow(() -> InformalNotificationStatusValidator.assertInformalCompatible(empty));
    }

    @Test
    void legalDeliveredStatusFailsFastWithDedicatedErrorCode() {
        assertFailsFast(UnifiedNotificationStatus.DELIVERED);
    }

    @Test
    void legalViewedStatusFailsFastWithDedicatedErrorCode() {
        assertFailsFast(UnifiedNotificationStatus.VIEWED);
    }

    @Test
    void incompatibleStatusIsDetectedEvenWhenNotFirstInPage() {
        NotificationSearchRow ok = baseRow(UnifiedNotificationStatus.PROCESSING);
        NotificationSearchRow ko = baseRow(UnifiedNotificationStatus.DELIVERED);

        PnInternalException ex = assertThrows(PnInternalException.class,
                () -> InformalNotificationStatusValidator.assertInformalCompatible(page(ok, ko)));
        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_INCOMPATIBLE_NOTIFICATION_STATUS,
                ex.getProblem().getErrors().get(0).getCode());
    }

    private void assertFailsFast(UnifiedNotificationStatus status) {
        PnInternalException ex = assertThrows(PnInternalException.class,
                () -> InformalNotificationStatusValidator.assertInformalCompatible(page(baseRow(status))));

        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_INCOMPATIBLE_NOTIFICATION_STATUS,
                ex.getProblem().getErrors().get(0).getCode());
        assertTrue(ex.getMessage().contains(status.getValue()));
    }

    @SafeVarargs
    private final ResultPaginationDto<NotificationSearchRow, String> page(NotificationSearchRow... rows) {
        return ResultPaginationDto.<NotificationSearchRow, String>builder()
                .resultsPage(new ArrayList<>(List.of(rows)))
                .build();
    }

    private NotificationSearchRow baseRow(UnifiedNotificationStatus status) {
        return NotificationSearchRow.builder()
                .iun("INFR-MLEL-VDDY-202209-A-1")
                .paProtocolNumber("protocol-1")
                .sender("comune di milano")
                .sentAt(OffsetDateTime.parse("2022-09-05T18:47:39.267123Z"))
                .subject("oggetto")
                .recipients(List.of("recipientId1"))
                .requestAcceptedAt(OffsetDateTime.parse("2022-09-05T18:47:39.267123Z"))
                .notificationStatus(status)
                .build();
    }
}
