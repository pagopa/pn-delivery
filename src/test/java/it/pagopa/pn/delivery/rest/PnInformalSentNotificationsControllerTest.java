package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.exception.PnForbiddenException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationSearchResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusV1;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.NotificationSearchCommunicationType;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.search.CampaignAuthValidator;
import it.pagopa.pn.delivery.svc.search.NotificationSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PnInformalSentNotificationsControllerTest {

    private static final String UID = "uid";
    private static final String CX_ID = "senderId";
    private static final String CAMPAIGN_ID = "campaignId";
    private static final String RECIPIENT_ID = "recipientId";
    private static final OffsetDateTime START = OffsetDateTime.parse("2022-05-01T00:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2022-05-30T00:00:00Z");

    private NotificationSearchService retrieveSvc;
    private ModelMapper modelMapper;
    private CampaignAuthValidator campaignAuthValidator;
    private PnInformalSentNotificationsController controller;

    @BeforeEach
    void setup() {
        this.retrieveSvc = Mockito.mock(NotificationSearchService.class);
        this.modelMapper = Mockito.mock(ModelMapper.class);
        this.campaignAuthValidator = Mockito.mock(CampaignAuthValidator.class);
        this.controller = new PnInformalSentNotificationsController(retrieveSvc, modelMapper, campaignAuthValidator);
    }

    @Test
    void searchInformalSentNotificationBuildsExpectedDto() {
        ResultPaginationDto<NotificationSearchRow, String> serviceResult = emptyPage();
        when(retrieveSvc.searchNotification(any(), any(), any())).thenReturn(serviceResult);
        InformalNotificationSearchResponse mapped = new InformalNotificationSearchResponse();
        when(modelMapper.map(eq(serviceResult), eq(InformalNotificationSearchResponse.class))).thenReturn(mapped);

        ResponseEntity<InformalNotificationSearchResponse> response = controller.searchInformalSentNotification(
                UID, CxTypeAuthFleet.PA, CX_ID, CAMPAIGN_ID, START, END, List.of("G1"),
                RECIPIENT_ID, null, InformalNotificationStatusV1.PROCESSING, true, false, 10, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(mapped, response.getBody());

        // l'auth campagna->mittente è invocata prima della ricerca
        Mockito.verify(campaignAuthValidator).checkCampaignIsFromSender(CAMPAIGN_ID, CX_ID);

        ArgumentCaptor<InputSearchNotificationDto> captor = ArgumentCaptor.forClass(InputSearchNotificationDto.class);
        Mockito.verify(retrieveSvc).searchNotification(captor.capture(), any(), any());
        InputSearchNotificationDto dto = captor.getValue();
        assertTrue(dto.isByCampaign());
        assertEquals(CAMPAIGN_ID, dto.getCampaignId());
        assertEquals(CX_ID, dto.getSenderReceiverId());
        assertEquals(RECIPIENT_ID, dto.getFilterId());
        assertEquals(NotificationSearchCommunicationType.INFORMAL, dto.getCommunicationType());
        assertEquals(List.of(InformalNotificationStatusV1.PROCESSING), dto.getInformalStatuses());
        assertEquals(Boolean.TRUE, dto.getViewed());
        assertEquals(Boolean.FALSE, dto.getDelivered());
        assertFalse(dto.isReceiverIdIsOpaque());
        assertEquals(10, dto.getSize());
    }

    @Test
    void emptyInformalStatusesWhenStatusNull() {
        ResultPaginationDto<NotificationSearchRow, String> serviceResult = emptyPage();
        when(retrieveSvc.searchNotification(any(), any(), any())).thenReturn(serviceResult);
        when(modelMapper.map(any(), eq(InformalNotificationSearchResponse.class)))
                .thenReturn(new InformalNotificationSearchResponse());

        controller.searchInformalSentNotification(
                UID, CxTypeAuthFleet.PA, CX_ID, CAMPAIGN_ID, START, END, List.of("G1"),
                RECIPIENT_ID, null, null, null, null, 10, null);

        ArgumentCaptor<InputSearchNotificationDto> captor = ArgumentCaptor.forClass(InputSearchNotificationDto.class);
        Mockito.verify(retrieveSvc).searchNotification(captor.capture(), any(), any());
        assertTrue(captor.getValue().getInformalStatuses().isEmpty());
    }

    @Test
    void forbiddenWhenCampaignDoesNotBelongToSender() {
        Mockito.doThrow(new PnForbiddenException("La campagna non appartiene al mittente"))
                .when(campaignAuthValidator).checkCampaignIsFromSender(CAMPAIGN_ID, CX_ID);

        assertThrows(PnForbiddenException.class, () -> controller.searchInformalSentNotification(
                UID, CxTypeAuthFleet.PA, CX_ID, CAMPAIGN_ID, START, END, List.of("G1"),
                RECIPIENT_ID, null, null,  null, null, 10, null));

        // la ricerca non viene eseguita se l'autorizzazione fallisce
        Mockito.verifyNoInteractions(retrieveSvc);
    }

    private ResultPaginationDto<NotificationSearchRow, String> emptyPage() {
        return ResultPaginationDto.<NotificationSearchRow, String>builder()
                .resultsPage(new ArrayList<>())
                .nextPagesKey(List.of())
                .build();
    }
}
