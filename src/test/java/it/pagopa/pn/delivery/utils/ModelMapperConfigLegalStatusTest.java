package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LegalNotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
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
 * Verifica l'invariante fail-fast del boundary legale (flusso mittente/delegato):
 * gli stati tipici delle bonarie che raggiungessero il boundary legale fanno fallire subito la
 * conversione con {@link PnInternalException} e il codice errore dedicato, mentre gli stati legali
 * (e {@code REFUSED}, condiviso) vengono mappati correttamente.
 */
class ModelMapperConfigLegalStatusTest {

    private ModelMapper modelMapper;

    @BeforeEach
    void setup() {
        this.modelMapper = new ModelMapperConfig().modelMapper();
    }

    @Test
    void legalStatusPassesValidationAndIsMappedToLegalRow() {
        NotificationSearchRow row = baseRow(UnifiedNotificationStatus.ACCEPTED);

        assertDoesNotThrow(() -> LegalNotificationStatusValidator.assertLegalCompatible(page(row)));

        LegalNotificationSearchRow mapped = modelMapper.map(row, LegalNotificationSearchRow.class);
        assertEquals(NotificationStatusV26.ACCEPTED, mapped.getNotificationStatus());
    }

    @Test
    void sharedRefusedStatusPassesValidationAndIsMappedToLegalRow() {
        NotificationSearchRow row = baseRow(UnifiedNotificationStatus.REFUSED);

        assertDoesNotThrow(() -> LegalNotificationStatusValidator.assertLegalCompatible(page(row)));

        LegalNotificationSearchRow mapped = modelMapper.map(row, LegalNotificationSearchRow.class);
        assertEquals(NotificationStatusV26.REFUSED, mapped.getNotificationStatus());
    }

    @Test
    void nullStatusIsTolerated() {
        NotificationSearchRow row = baseRow(null);

        assertDoesNotThrow(() -> LegalNotificationStatusValidator.assertLegalCompatible(page(row)));
    }

    @Test
    void nullServiceResultIsTolerated() {
        assertDoesNotThrow(() -> LegalNotificationStatusValidator.assertLegalCompatible(null));
    }

    @Test
    void emptyResultsPageIsTolerated() {
        ResultPaginationDto<NotificationSearchRow, String> empty = ResultPaginationDto
                .<NotificationSearchRow, String>builder()
                .resultsPage(new ArrayList<>())
                .build();

        assertDoesNotThrow(() -> LegalNotificationStatusValidator.assertLegalCompatible(empty));
    }

    @Test
    void informalReadyToSendStatusFailsFastWithDedicatedErrorCode() {
        assertFailsFast(UnifiedNotificationStatus.READY_TO_SEND);
    }

    @Test
    void informalProcessingStatusFailsFastWithDedicatedErrorCode() {
        assertFailsFast(UnifiedNotificationStatus.PROCESSING);
    }

    @Test
    void informalSuccessfulSendingStatusFailsFastWithDedicatedErrorCode() {
        assertFailsFast(UnifiedNotificationStatus.SUCCESSFUL_SENDING);
    }

    @Test
    void informalUnsuccessfulSendingStatusFailsFastWithDedicatedErrorCode() {
        assertFailsFast(UnifiedNotificationStatus.UNSUCCESSFUL_SENDING);
    }

    @Test
    void incompatibleStatusIsDetectedEvenWhenNotFirstInPage() {
        NotificationSearchRow ok = baseRow(UnifiedNotificationStatus.DELIVERED);
        NotificationSearchRow ko = baseRow(UnifiedNotificationStatus.PROCESSING);

        PnInternalException ex = assertThrows(PnInternalException.class,
                () -> LegalNotificationStatusValidator.assertLegalCompatible(page(ok, ko)));
        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_INCOMPATIBLE_NOTIFICATION_STATUS,
                ex.getProblem().getErrors().get(0).getCode());
    }

    private void assertFailsFast(UnifiedNotificationStatus status) {
        NotificationSearchRow row = baseRow(status);

        PnInternalException ex = assertThrows(PnInternalException.class,
                () -> LegalNotificationStatusValidator.assertLegalCompatible(page(row)));

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
                .iun("TGWR-ZJQN-JMAR-202209-A-1")
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
