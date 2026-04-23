package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationRequestV1;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.CommunicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelMapperConfigTest {

    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapperConfig().modelMapper();
    }

    @Test
    void mapsInformalNotificationRequestAndSetsCommunicationTypeToInformal() {
        InformalNotificationRequestV1 request = InformalNotificationRequestV1.builder()
                .senderTaxId("12345678901")
                .senderDenomination("Comune di Test")
                .paProtocolNumber("protocol-1")
                .idempotenceToken("idem-1")
                .campaignId("campaign-1")
                .subject("subject")
                .group("group-1")
                .build();

        InternalNotification result = modelMapper.map(request, InternalNotification.class);

        assertEquals("12345678901", result.getSenderTaxId());
        assertEquals("Comune di Test", result.getSenderDenomination());
        assertEquals("protocol-1", result.getPaProtocolNumber());
        assertEquals("idem-1", result.getIdempotenceToken());
        assertEquals("campaign-1", result.getCampaignId());
        assertEquals("subject", result.getSubject());
        assertEquals("group-1", result.getGroup());
        assertEquals(CommunicationType.INFORMAL, result.getCommunicationType());
    }

    @Test
    void setsCommunicationTypeToInformalEvenWhenSourceIsEmpty() {
        InformalNotificationRequestV1 request = InformalNotificationRequestV1.builder().build();

        InternalNotification result = modelMapper.map(request, InternalNotification.class);

        assertEquals(CommunicationType.INFORMAL, result.getCommunicationType());
    }
}

