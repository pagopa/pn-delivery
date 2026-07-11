package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.models.SenderContactsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SenderContactsServiceImplTest {

    @Mock
    private PnDeliveryConfigs cfg;

    @InjectMocks
    private SenderContactsServiceImpl senderContactsService;

    @Test
    void getSenderContacts_WhenSenderIdExists_ShouldReturnCorrectContacts() {
        // Arrange
        String targetSenderId = "SENDER_A";

        SenderContactsDto contactA = SenderContactsDto.builder()
                .senderId(targetSenderId)
                .email("info@senderA.com")
                .build();

        SenderContactsDto contactB = SenderContactsDto.builder()
                .senderId("SENDER_B")
                .email("info@senderB.com")
                .build();

        // Configura il mock dello store per restituire una lista con due contatti
        when(cfg.getSenderContacts()).thenReturn(List.of(contactA, contactB));

        // Act
        SenderContactsDto result = senderContactsService.getSenderContacts(targetSenderId);

        // Assert
        assertNotNull(result);
        assertEquals(targetSenderId, result.getSenderId());
        assertEquals("info@senderA.com", result.getEmail());
        verify(cfg).getSenderContacts();
    }

    @Test
    void getSenderContacts_WhenSenderIdDoesNotExist_ShouldThrowPnNotFoundException() {
        // Arrange
        String unknownSenderId = "UNKNOWN_SENDER";

        SenderContactsDto existingContact = SenderContactsDto.builder()
                .senderId("SENDER_EXISTING")
                .email("info@existing.com")
                .build();

        when(cfg.getSenderContacts()).thenReturn(List.of(existingContact));

        // Act & Assert
        assertThrows(PnNotFoundException.class, () -> senderContactsService.getSenderContacts(unknownSenderId));
    }

    @Test
    void getSenderContacts_WhenStoreIsEmpty_ShouldThrowPnNotFoundException() {
        // Arrange
        String anySenderId = "ANY_SENDER";

        // Simula il comportamento dello store quando è vuoto
        when(cfg.getSenderContacts()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(PnNotFoundException.class, () -> senderContactsService.getSenderContacts(anySenderId));
    }
}