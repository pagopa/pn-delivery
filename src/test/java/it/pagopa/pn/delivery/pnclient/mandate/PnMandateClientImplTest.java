package it.pagopa.pn.delivery.pnclient.mandate;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.DelegateType;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.MandateByDelegatorRequestDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PnMandateClientImplTest {

    private static final String DELEGATE = "delegate";
    private static final String DELEGATOR = "delegator";
    private static final String MANDATE_ID = "mandate_id";
    private static final String MANDATE_DATE_FROM = "2022-04-22T16:00:00Z";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryConfigs cfg;

    private PnMandateClientImpl mandateClient;

    @BeforeEach
    void setup() {
        this.cfg = mock(PnDeliveryConfigs.class);
        when(cfg.getMandateBaseUrl()).thenReturn("http://localhost:8080");
        this.mandateClient = new PnMandateClientImpl(restTemplate, cfg);
    }

    @Test
    void getMandatesSuccess() {
        //Given
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setDelegate(DELEGATE);
        internalMandateDto.setDelegator(DELEGATOR);
        internalMandateDto.mandateId(MANDATE_ID);
        internalMandateDto.setDatefrom(MANDATE_DATE_FROM);
        ResponseEntity<List<InternalMandateDto>> response = ResponseEntity.ok(List.of(internalMandateDto));

        //When
        when(restTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);
        List<InternalMandateDto> result = mandateClient.listMandatesByDelegate(DELEGATE, MANDATE_ID, CxTypeAuthFleet.PF, null);

        //Then
        Assertions.assertNotNull(result);
    }

    @Test
    void getMandatesEmpty() {
        ResponseEntity<List<InternalMandateDto>> response = ResponseEntity.ok(Collections.emptyList());

        when(restTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);
        List<InternalMandateDto> result = mandateClient.listMandatesByDelegate(DELEGATE, MANDATE_ID, CxTypeAuthFleet.PF, null);

        Assertions.assertNotNull(result);
    }

    @Test
    void getMandatesByDelegatorSuccess() {
        //Given
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setDelegate(DELEGATE);
        internalMandateDto.setDelegator(DELEGATOR);
        internalMandateDto.mandateId(MANDATE_ID);
        internalMandateDto.setDatefrom(MANDATE_DATE_FROM);
        ResponseEntity<List<InternalMandateDto>> response = ResponseEntity.ok(List.of(internalMandateDto));

        //When
        when(restTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);
        List<InternalMandateDto> result = mandateClient.listMandatesByDelegator(DELEGATE, MANDATE_ID, CxTypeAuthFleet.PF, null, null, null);

        //Then
        Assertions.assertNotNull(result);
    }

    @Test
    void getMandatesByDelegatorsSuccess() {
        //Given
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setDelegate(DELEGATE);
        internalMandateDto.setDelegator(DELEGATOR);
        internalMandateDto.mandateId(MANDATE_ID);
        internalMandateDto.setDatefrom(MANDATE_DATE_FROM);
        ResponseEntity<List<InternalMandateDto>> response = ResponseEntity.ok(List.of(internalMandateDto));

        MandateByDelegatorRequestDto requestDto = new MandateByDelegatorRequestDto();
        requestDto.setMandateId(MANDATE_ID);
        requestDto.setDelegatorId(DELEGATOR);
        List<MandateByDelegatorRequestDto> requestBody = List.of(requestDto);

        //When
        when(restTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);
        List<InternalMandateDto> result = mandateClient.listMandatesByDelegators(DelegateType.PG, null, requestBody);

        //Then
        Assertions.assertNotNull(result);
    }

}
