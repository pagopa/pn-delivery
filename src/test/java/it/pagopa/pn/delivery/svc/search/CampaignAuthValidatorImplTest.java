package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.svc.CampaignService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CampaignAuthValidatorImplTest {

    private static final String CAMPAIGN_ID = "CAMP-TEST-01";
    private static final String SENDER_ID = "5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce";

    private CampaignService campaignService;
    private CampaignAuthValidatorImpl campaignAuthValidator;

    @BeforeEach
    void setup() {
        campaignService = Mockito.mock(CampaignService.class);
        campaignAuthValidator = new CampaignAuthValidatorImpl(campaignService);
    }

    @Test
    void checkCampaignIsFromSender_doesNotThrow_whenCampaignBelongsToSender() {
        // Arrange
        Mockito.when(campaignService.getCampaign(CAMPAIGN_ID, SENDER_ID))
                .thenReturn(new CampaignDetail());

        // Act & Assert
        Assertions.assertDoesNotThrow(() ->
                campaignAuthValidator.checkCampaignIsFromSender(CAMPAIGN_ID, SENDER_ID));
        Mockito.verify(campaignService).getCampaign(CAMPAIGN_ID, SENDER_ID);
    }

    @Test
    void checkCampaignIsFromSender_throwsPnCampaignNotFoundException_whenCampaignDoesNotBelongToSender() {
        // Arrange
        Mockito.when(campaignService.getCampaign(CAMPAIGN_ID, SENDER_ID))
                .thenThrow(new PnCampaignNotFoundException(
                        String.format("Campaign with campaignId=%s and senderId=%s not found", CAMPAIGN_ID, SENDER_ID)));

        // Act & Assert
        Assertions.assertThrows(PnCampaignNotFoundException.class, () ->
                campaignAuthValidator.checkCampaignIsFromSender(CAMPAIGN_ID, SENDER_ID));
    }

    @Test
    void checkCampaignIsFromSender_throwsPnCampaignNotFoundException_whenCampaignDoesNotExistAtAll() {
        // Arrange: senderId "sconosciuto", nessuna campagna per esso configurata
        String unknownSenderId = "00000000-0000-0000-0000-000000000000";
        Mockito.when(campaignService.getCampaign(CAMPAIGN_ID, unknownSenderId))
                .thenThrow(new PnCampaignNotFoundException(
                        String.format("Campaign with campaignId=%s and senderId=%s not found", CAMPAIGN_ID, unknownSenderId)));

        // Act & Assert
        Assertions.assertThrows(PnCampaignNotFoundException.class, () ->
                campaignAuthValidator.checkCampaignIsFromSender(CAMPAIGN_ID, unknownSenderId));
    }
}
