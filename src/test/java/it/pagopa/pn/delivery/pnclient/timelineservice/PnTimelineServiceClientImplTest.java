package it.pagopa.pn.delivery.pnclient.timelineservice;

import it.pagopa.pn.delivery.generated.openapi.msclient.timelineservice.v1.api.TimelineControllerApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.timelineservice.v1.model.DeliveryInformationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PnTimelineServiceClientImplTest {

    @Mock
    private TimelineControllerApi timelineControllerApi;

    @InjectMocks
    private PnTimelineServiceClientImpl pnTimelineServiceClientImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDeliveryInformation_shouldReturnResponse() {
        String iun = "test-iun";
        Integer recIndex = 1;
        DeliveryInformationResponse mockResponse = mock(DeliveryInformationResponse.class);
        when(timelineControllerApi.getDeliveryInformation(iun, recIndex)).thenReturn(mockResponse);

        DeliveryInformationResponse result = pnTimelineServiceClientImpl.getDeliveryInformation(iun, recIndex);

        assertNotNull(result);
        assertEquals(mockResponse, result);
        verify(timelineControllerApi, times(1)).getDeliveryInformation(iun, recIndex);
    }
}
