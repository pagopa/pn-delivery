package it.pagopa.pn.delivery.svc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnMandateEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationDelegationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;

import java.time.Instant;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {NotificationDelegatedService.class})
@ExtendWith(SpringExtension.class)
class NotificationDelegatedServiceTest {

    private static final String MANDATE_ID = "mandateId";
    private static final String DELEGATE_ID = "delegateId";
    private static final String DELEGATOR_ID = "delegatorId";

    @Autowired
    private NotificationDelegatedService notificationDelegatedService;

    @MockBean
    private NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao;

    @MockBean
    private NotificationMetadataEntityDao notificationMetadataEntityDao;

    @MockBean
    private PnMandateClientImpl pnMandateClientImpl;

    /**
     * Method under test: {@link NotificationDelegatedService#handleAcceptedMandate(PnMandateEvent.Payload, EventType)}
     */
    @Test
    @DisplayName("Test mandate not found")
    void testHandleAcceptedMandate() {
        when(pnMandateClientImpl.listMandatesByDelegator(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());
        PnMandateEvent.Payload payload = new PnMandateEvent.Payload();
        assertThrows(PnInternalException.class, () -> notificationDelegatedService
                .handleAcceptedMandate(payload, EventType.MANDATE_ACCEPTED));
        verify(pnMandateClientImpl).listMandatesByDelegator(any(), any(), any(), any(), any(), any());
    }

    /**
     * Method under test: {@link NotificationDelegatedService#handleAcceptedMandate(PnMandateEvent.Payload, EventType)}
     */
    @Test
    @DisplayName("Test duplication")
    @Disabled( "Richiesta modifica a Martelli" )
    void testHandleAcceptedMandate2() {
        Instant mandateStartValidity = Instant.now().minus(1, ChronoUnit.DAYS);
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId(MANDATE_ID);
        internalMandateDto.setDelegator(DELEGATOR_ID);
        internalMandateDto.setDelegate(DELEGATE_ID);
        internalMandateDto.setDatefrom(mandateStartValidity.toString());
        List<InternalMandateDto> internalMandateDtoList = List.of(internalMandateDto);
        when(pnMandateClientImpl.listMandatesByDelegator(any(), any(), any(), any(), any(), any()))
                .thenReturn(internalMandateDtoList);

        PageSearchTrunk<NotificationMetadataEntity> page = new PageSearchTrunk<>();
        page.setResults(List.of(NotificationMetadataEntity.builder().sentAt(Instant.now()).build()));
        when(notificationMetadataEntityDao.searchForOneMonth(any(), any(), any(), anyInt(), any()))
                .thenReturn(page);
        when(notificationDelegationMetadataEntityDao.batchPutItems(anyList()))
                .thenReturn(Collections.emptyList());

        PnMandateEvent.Payload payload = PnMandateEvent.Payload.builder()
                .mandateId(MANDATE_ID)
                .delegatorId(DELEGATOR_ID)
                .delegateId(DELEGATE_ID)
                .validFrom(mandateStartValidity)
                .build();
        assertDoesNotThrow(() -> notificationDelegatedService.handleAcceptedMandate(payload, EventType.MANDATE_ACCEPTED));

        verify(pnMandateClientImpl).listMandatesByDelegator(any(), any(), any(), any(), any(), any());
        verify(notificationMetadataEntityDao).searchForOneMonth(any(), any(), any(), anyInt(), any());
        verify(notificationDelegationMetadataEntityDao).batchPutItems(anyList());
    }

    @Test
    @DisplayName("Test unprocessed items in batch put items")
    void testHandleAcceptedMandate3() {
        Instant mandateStartValidity = Instant.now().minus(1, ChronoUnit.DAYS);
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId(MANDATE_ID);
        internalMandateDto.setDelegator(DELEGATOR_ID);
        internalMandateDto.setDelegate(DELEGATE_ID);
        internalMandateDto.setDatefrom(mandateStartValidity.toString());
        List<InternalMandateDto> internalMandateDtoList = List.of(internalMandateDto);
        when(pnMandateClientImpl.listMandatesByDelegator(any(), any(), any(), any(), any(), any()))
                .thenReturn(internalMandateDtoList);

        PageSearchTrunk<NotificationMetadataEntity> page = new PageSearchTrunk<>();
        page.setResults(List.of(NotificationMetadataEntity.builder().sentAt(Instant.now()).build()));
        when(notificationMetadataEntityDao.searchForOneMonth(any(), any(), any(), anyInt(), any()))
                .thenReturn(page);
        when(notificationDelegationMetadataEntityDao.batchPutItems(anyList()))
                .thenReturn(List.of(new NotificationDelegationMetadataEntity()));

        PnMandateEvent.Payload payload = PnMandateEvent.Payload.builder()
                .mandateId(MANDATE_ID)
                .delegatorId(DELEGATOR_ID)
                .delegateId(DELEGATE_ID)
                .validFrom(mandateStartValidity)
                .build();
        assertThrows(PnInternalException.class, () -> notificationDelegatedService.handleAcceptedMandate(payload, EventType.MANDATE_ACCEPTED));

        verify(pnMandateClientImpl).listMandatesByDelegator(any(), any(), any(), any(), any(), any());
        verify(notificationMetadataEntityDao).searchForOneMonth(any(), any(), any(), anyInt(), any());
        verify(notificationDelegationMetadataEntityDao).batchPutItems(anyList());
    }

    @Test
    @DisplayName("Test query pagination")
    @Disabled( "Richiesta modifica a Martelli" )
    void testHandleAcceptedMandate4() {
        Instant mandateStartValidity = Instant.now().minus(1, ChronoUnit.DAYS);
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId(MANDATE_ID);
        internalMandateDto.setDelegator(DELEGATOR_ID);
        internalMandateDto.setDelegate(DELEGATE_ID);
        internalMandateDto.setDatefrom(mandateStartValidity.toString());
        List<InternalMandateDto> internalMandateDtoList = List.of(internalMandateDto);
        when(pnMandateClientImpl.listMandatesByDelegator(any(), any(), any(), any(), any(), any()))
                .thenReturn(internalMandateDtoList);

        PageSearchTrunk<NotificationMetadataEntity> page1 = new PageSearchTrunk<>();
        page1.setResults(List.of(NotificationMetadataEntity.builder().sentAt(Instant.now()).build()));
        page1.setLastEvaluatedKey(Collections.emptyMap());
        PageSearchTrunk<NotificationMetadataEntity> page2 = new PageSearchTrunk<>();
        page2.setResults(List.of(NotificationMetadataEntity.builder().sentAt(Instant.now()).build()));
        when(notificationMetadataEntityDao.searchForOneMonth(any(), any(), any(), anyInt(), any()))
                .thenReturn(page1)
                .thenReturn(page2);
        when(notificationDelegationMetadataEntityDao.batchPutItems(anyList()))
                .thenReturn(Collections.emptyList());

        PnMandateEvent.Payload payload = PnMandateEvent.Payload.builder()
                .mandateId(MANDATE_ID)
                .delegatorId(DELEGATOR_ID)
                .delegateId(DELEGATE_ID)
                .validFrom(mandateStartValidity)
                .build();
        assertDoesNotThrow(() -> notificationDelegatedService.handleAcceptedMandate(payload, EventType.MANDATE_ACCEPTED));

        verify(pnMandateClientImpl).listMandatesByDelegator(any(), any(), any(), any(), any(), any());
        verify(notificationMetadataEntityDao, times(2)).searchForOneMonth(any(), any(), any(), anyInt(), any());
        verify(notificationDelegationMetadataEntityDao, times(2)).batchPutItems(anyList());
    }

    @Test
    void deleteNotificationDelegatedSuccessTest() {
        NotificationDelegationMetadataEntity entity = NotificationDelegationMetadataEntity
                .builder()
                .mandateId(MANDATE_ID)
                .sentAt(OffsetDateTime.MAX.toInstant())
                .iunRecipientIdDelegateIdGroupId("partitionValueTest")
                .build();

        List<NotificationDelegationMetadataEntity> resultsList = List.of(entity);
        PageSearchTrunk<NotificationDelegationMetadataEntity> results = new PageSearchTrunk<>();
        results.setResults(resultsList);
        results.setLastEvaluatedKey(null);
        when(notificationDelegationMetadataEntityDao.searchDelegatedByMandateId(anyString(), anyInt(), any()))
                .thenReturn(results);

        assertDoesNotThrow(() -> notificationDelegatedService.deleteNotificationDelegatedByMandateId(MANDATE_ID, EventType.MANDATE_REJECTED));

        verify(notificationDelegationMetadataEntityDao).searchDelegatedByMandateId(anyString(), anyInt(), any());
        verify(notificationDelegationMetadataEntityDao).deleteWithConditions(any());
    }

    @Test
    void deleteNotificationDelegatedPaginationSuccessTest() {
        NotificationDelegationMetadataEntity entity = NotificationDelegationMetadataEntity
                .builder()
                .mandateId(MANDATE_ID)
                .sentAt(OffsetDateTime.MAX.toInstant())
                .iunRecipientIdDelegateIdGroupId("partitionValueTest")
                .build();

        List<NotificationDelegationMetadataEntity> resultsList = List.of(entity);
        PageSearchTrunk<NotificationDelegationMetadataEntity> results1 = new PageSearchTrunk<>();
        results1.setResults(resultsList);
        results1.setLastEvaluatedKey(Collections.emptyMap());
        PageSearchTrunk<NotificationDelegationMetadataEntity> results2 = new PageSearchTrunk<>();
        results2.setResults(resultsList);
        results2.setLastEvaluatedKey(null);
        when(notificationDelegationMetadataEntityDao.searchDelegatedByMandateId(anyString(), anyInt(), any()))
                .thenReturn(results1)
                .thenReturn(results2);

        assertDoesNotThrow(() -> notificationDelegatedService.deleteNotificationDelegatedByMandateId(MANDATE_ID, EventType.MANDATE_REJECTED));

        verify(notificationDelegationMetadataEntityDao, times(2)).searchDelegatedByMandateId(anyString(), anyInt(), any());
        verify(notificationDelegationMetadataEntityDao, times(2)).deleteWithConditions(any());
    }
}
