package it.pagopa.pn.delivery.pnclient.datavault;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.AddressDto;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PnDataVaultClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryConfigs cfg;

    private PnDataVaultClientImpl dataVaultClient;

    @BeforeEach
    void setup() {
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );
        Mockito.when( cfg.getDataVaultBaseUrl() ).thenReturn( "http://localhost:8080" );
        this.dataVaultClient = new PnDataVaultClientImpl( restTemplate, cfg );
    }

    @Test
    void ensureRecipientByExternalId() {
        //Given
        ResponseEntity<String > response = ResponseEntity.ok( UUID.randomUUID().toString() );

        //When
        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);
        String result = dataVaultClient.ensureRecipientByExternalId (RecipientType.PF, "RSSMRA85T10A562S" );

        //Then
        Assertions.assertNotNull( result );
    }

    @Test
    void updateNotificationAddressesByIun() {
        //Given
        ResponseEntity response = ResponseEntity.ok().build();
        NotificationRecipientAddressesDto recipientAddressesDto = new NotificationRecipientAddressesDto();
        recipientAddressesDto.setDenomination("denominazione");
        recipientAddressesDto.setDigitalAddress(new AddressDto());

        //When
        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        assertDoesNotThrow(() -> {
            dataVaultClient.updateNotificationAddressesByIun (UUID.randomUUID().toString(), List.of(recipientAddressesDto));
        });
        //Then

    }

    @Test
    void getRecipientDenominationByInternalId() {
        //Given
        String internalid = UUID.randomUUID().toString();
        BaseRecipientDto recipientAddressesDto = new BaseRecipientDto();
        recipientAddressesDto.setDenomination("denominazione");
        recipientAddressesDto.setTaxId("123123123");
        recipientAddressesDto.setRecipientType(RecipientType.PF);
        recipientAddressesDto.setInternalId(internalid);
        ResponseEntity<List<BaseRecipientDto>> response = ResponseEntity.ok(List.of(recipientAddressesDto));

        //When
        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        List<BaseRecipientDto> result =  dataVaultClient.getRecipientDenominationByInternalId (List.of(internalid));

        //Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getNotificationAddressesByIun() {
        //Given
        NotificationRecipientAddressesDto recipientAddressesDto = new NotificationRecipientAddressesDto();
        recipientAddressesDto.setDenomination("denominazione");
        recipientAddressesDto.setDigitalAddress(new AddressDto());
        ResponseEntity<List<NotificationRecipientAddressesDto>> response = ResponseEntity.ok(List.of(recipientAddressesDto));

        //When
        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        List<NotificationRecipientAddressesDto> result =  dataVaultClient.getNotificationAddressesByIun (UUID.randomUUID().toString());

        //Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}