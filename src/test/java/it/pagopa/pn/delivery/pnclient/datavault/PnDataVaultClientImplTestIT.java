package it.pagopa.pn.delivery.pnclient.datavault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.delivery.MockAWSObjectsTest;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery.data-vault-base-url=http://localhost:9998",
        "pn.delivery.max-recipients-count=0",
        "pn.delivery.max-attachments-count=0"
})
class PnDataVaultClientImplTestIT extends MockAWSObjectsTest {

    @Autowired
    private PnDataVaultClientImpl dataVaultClient;
    @Autowired
    private ObjectMapper objectMapper;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {

        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }


    @Test
    void ensureRecipientByExternalId() {
        //Given
        String path = "/datavault-private/v1/recipients/external/PF";

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                        .withBody("RSSMRA85T10A562S")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody( UUID.randomUUID().toString() )
                );
        //When
        dataVaultClient.ensureRecipientByExternalId (RecipientType.PF, "RSSMRA85T10A562S" );

        //Then
        assertDoesNotThrow( () -> {
            dataVaultClient.ensureRecipientByExternalId (RecipientType.PF, "RSSMRA85T10A562S" );
        });
    }

    @Test
    void updateNotificationAddressesByIun() throws JsonProcessingException {
        String path = "/datavault-private/v1/notifications/DHUJ-QYVT-DMVH-202302-P-1/addresses";
        //Given

        NotificationRecipientAddressesDto recipientAddressesDto = new NotificationRecipientAddressesDto();
        recipientAddressesDto.setDenomination("denominazione");
        recipientAddressesDto.setDigitalAddress(new AddressDto().value("address"));
        recipientAddressesDto.setRecIndex(0);
        recipientAddressesDto.setPhysicalAddress(new AnalogDomicile()
                .at("at")
                .address("address")
                .addressDetails("addrDet")
                .cap("cap")
                .municipality("mun")
                .municipalityDetails("munDet")
                .province("prov")
                .state("state"));


        //When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(path)
                        .withBody(objectMapper.writeValueAsString(List.of(recipientAddressesDto)))
                )
                .respond(response()
                        .withStatusCode(200)
                );

        dataVaultClient.updateNotificationAddressesByIun ("DHUJ-QYVT-DMVH-202302-P-1", List.of(recipientAddressesDto));

        assertDoesNotThrow(() -> {
            dataVaultClient.updateNotificationAddressesByIun ("DHUJ-QYVT-DMVH-202302-P-1", List.of(recipientAddressesDto));
        });

    }

    @Test
    void getRecipientDenominationByInternalId() throws JsonProcessingException {
        //Given
        String path = "/datavault-private/v1/recipients/internal";
        String internalid = UUID.randomUUID().toString();
        BaseRecipientDto recipientAddressesDto = new BaseRecipientDto();
        recipientAddressesDto.setDenomination("denominazione");
        recipientAddressesDto.setTaxId("123123123");
        recipientAddressesDto.setRecipientType(RecipientType.PF);
        recipientAddressesDto.setInternalId(internalid);

        //When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                        .withQueryStringParameter("internalId",internalid)
                )
                .respond(response()
                        .withStatusCode(200)
                );

        dataVaultClient.getRecipientDenominationByInternalId (List.of(internalid));

        //Then
        assertDoesNotThrow( () -> {
            dataVaultClient.getRecipientDenominationByInternalId (List.of(internalid));
        });

    }

    @Test
    void getNotificationAddressesByIun() {
        //Given
        String path = "/datavault-private/v1/notifications/DHUJ-QYVT-DMVH-202302-P-1/addresses";
        NotificationRecipientAddressesDto recipientAddressesDto = new NotificationRecipientAddressesDto();
        recipientAddressesDto.setDenomination("denominazione");
        recipientAddressesDto.setDigitalAddress(new AddressDto());
        //When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );

        dataVaultClient.getNotificationAddressesByIun ("DHUJ-QYVT-DMVH-202302-P-1");

        //Then
        assertDoesNotThrow( () -> {
            dataVaultClient.getNotificationAddressesByIun ("DHUJ-QYVT-DMVH-202302-P-1");
        });
    }
}