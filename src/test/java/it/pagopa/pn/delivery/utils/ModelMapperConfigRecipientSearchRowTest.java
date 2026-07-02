package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CommunicationOutcomes;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullNotificationSearchResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullNotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.UnifiedNotificationStatus;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test del converter WI-US2.9 che mappa la riga di ricerca interna
 * {@link NotificationSearchRow} sul DTO destinatario {@link FullNotificationSearchRow}.
 */
class ModelMapperConfigRecipientSearchRowTest {

    private ModelMapper modelMapper;

    @BeforeEach
    void setup() {
        this.modelMapper = new ModelMapperConfig().modelMapper();
    }

    @Test
    void communicationTypeDefaultsToLegalWhenNull() {
        NotificationSearchRow source = baseRow().toBuilder().communicationType(null).build();

        FullNotificationSearchRow result = modelMapper.map(source, FullNotificationSearchRow.class);

        assertEquals(FullNotificationSearchRow.CommunicationTypeEnum.LEGAL, result.getCommunicationType());
    }

    @Test
    void communicationTypeDefaultsToLegalWhenBlank() {
        NotificationSearchRow source = baseRow().toBuilder().communicationType("").build();

        FullNotificationSearchRow result = modelMapper.map(source, FullNotificationSearchRow.class);

        assertEquals(FullNotificationSearchRow.CommunicationTypeEnum.LEGAL, result.getCommunicationType());
    }

    @Test
    void communicationTypeMappedToLegal() {
        NotificationSearchRow source = baseRow().toBuilder().communicationType("LEGAL").build();

        FullNotificationSearchRow result = modelMapper.map(source, FullNotificationSearchRow.class);

        assertEquals(FullNotificationSearchRow.CommunicationTypeEnum.LEGAL, result.getCommunicationType());
    }

    @Test
    void communicationTypeMappedToInformal() {
        NotificationSearchRow source = baseRow().toBuilder().communicationType("INFORMAL").build();

        FullNotificationSearchRow result = modelMapper.map(source, FullNotificationSearchRow.class);

        assertEquals(FullNotificationSearchRow.CommunicationTypeEnum.INFORMAL, result.getCommunicationType());
    }

    @Test
    void communicationOutcomesPopulatedWhenViewedAndDeliveredPresent() {
        NotificationSearchRow source = baseRow().toBuilder()
                .communicationType("INFORMAL")
                .viewed(true)
                .delivered(false)
                .build();

        FullNotificationSearchRow result = modelMapper.map(source, FullNotificationSearchRow.class);

        CommunicationOutcomes outcomes = result.getCommunicationOutcomes();
        assertNotNull(outcomes);
        assertEquals(Boolean.TRUE, outcomes.getViewed());
        assertEquals(Boolean.FALSE, outcomes.getDelivered());
    }

    @Test
    void communicationOutcomesPopulatedWhenOnlyViewedPresent() {
        NotificationSearchRow source = baseRow().toBuilder()
                .communicationType("INFORMAL")
                .viewed(true)
                .delivered(null)
                .build();

        FullNotificationSearchRow result = modelMapper.map(source, FullNotificationSearchRow.class);

        CommunicationOutcomes outcomes = result.getCommunicationOutcomes();
        assertNotNull(outcomes);
        assertEquals(Boolean.TRUE, outcomes.getViewed());
        assertNull(outcomes.getDelivered());
    }

    @Test
    void communicationOutcomesNullForLegacyLegalNotification() {
        // notifica legale storica: nessun dato dinamico
        NotificationSearchRow source = baseRow().toBuilder()
                .communicationType("LEGAL")
                .viewed(null)
                .delivered(null)
                .build();

        FullNotificationSearchRow result = modelMapper.map(source, FullNotificationSearchRow.class);

        assertNull(result.getCommunicationOutcomes());
    }

    @Test
    void desiredFeedbackIsNotExposedOnOutput() {
        NotificationSearchRow source = baseRow().toBuilder()
                .communicationType("INFORMAL")
                .viewed(true)
                .delivered(true)
                .desiredFeedback(true)
                .build();

        FullNotificationSearchRow result = modelMapper.map(source, FullNotificationSearchRow.class);

        // il DTO destinatario non espone desiredFeedback: garanzia a compile-time + nessun metodo a runtime
        assertTrue(java.util.Arrays.stream(FullNotificationSearchRow.class.getMethods())
                .noneMatch(m -> m.getName().toLowerCase().contains("desiredfeedback")));
        assertNotNull(result.getCommunicationOutcomes());
    }

    @Test
    void baseFieldsArePreservedByConverter() {
        NotificationSearchRow source = baseRow().toBuilder().communicationType("LEGAL").build();

        FullNotificationSearchRow result = modelMapper.map(source, FullNotificationSearchRow.class);

        assertEquals(source.getIun(), result.getIun());
        assertEquals(source.getPaProtocolNumber(), result.getPaProtocolNumber());
        assertEquals(source.getSender(), result.getSender());
        assertEquals(source.getSubject(), result.getSubject());
        assertEquals(source.getNotificationStatus(), result.getNotificationStatus());
    }

    @Test
    void paginationWrapperIsMappedToResponse() {
        // come fa il controller: ResultPaginationDto<NotificationSearchRow,String> -> FullNotificationSearchResponse
        NotificationSearchRow legal = baseRow().toBuilder().communicationType("LEGAL").build();
        NotificationSearchRow informal = baseRow().toBuilder()
                .iun("INFR-MLEL-VDDY-202209-A-2")
                .communicationType("INFORMAL")
                .viewed(true)
                .delivered(false)
                .build();

        ResultPaginationDto<NotificationSearchRow, String> page = new ResultPaginationDto<>();
        page.setResultsPage(List.of(legal, informal));
        page.setMoreResult(false);
        page.setNextPagesKey(List.of());

        FullNotificationSearchResponse response =
                modelMapper.map(page, FullNotificationSearchResponse.class);

        assertNotNull(response.getResultsPage());
        assertEquals(2, response.getResultsPage().size());

        FullNotificationSearchRow mappedLegal = response.getResultsPage().get(0);
        assertEquals(FullNotificationSearchRow.CommunicationTypeEnum.LEGAL, mappedLegal.getCommunicationType());
        assertNull(mappedLegal.getCommunicationOutcomes());

        FullNotificationSearchRow mappedInformal = response.getResultsPage().get(1);
        assertEquals(FullNotificationSearchRow.CommunicationTypeEnum.INFORMAL, mappedInformal.getCommunicationType());
        assertNotNull(mappedInformal.getCommunicationOutcomes());
        assertEquals(Boolean.TRUE, mappedInformal.getCommunicationOutcomes().getViewed());
        assertEquals(Boolean.FALSE, mappedInformal.getCommunicationOutcomes().getDelivered());
    }

    private NotificationSearchRow baseRow() {
        return NotificationSearchRow.builder()
                .iun("TGWR-ZJQN-JMAR-202209-A-1")
                .paProtocolNumber("protocol-1")
                .sender("comune di milano")
                .sentAt(OffsetDateTime.parse("2022-09-05T18:47:39.267123Z"))
                .subject("oggetto")
                .recipients(List.of("recipientId1"))
                .requestAcceptedAt(OffsetDateTime.parse("2022-09-05T18:47:39.267123Z"))
                .notificationStatus(UnifiedNotificationStatus.ACCEPTED)
                .build();
    }
}
