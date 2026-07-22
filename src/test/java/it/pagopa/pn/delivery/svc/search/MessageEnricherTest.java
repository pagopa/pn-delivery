package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MessageEnricherTest {
    @Mock
    private PnDataVaultClientImpl pnDataVaultClient;

    @InjectMocks
    private MessageEnricher messageEnricher;

    private static final String SENDER_PA_ID = UUID.randomUUID().toString();
    private static final String MESSAGE_ID = UUID.randomUUID().toString();
    private static final String ANOTHER_MESSAGE_ID = UUID.randomUUID().toString();

    @Test
    void enrichInternalNotification_withValidRecipient_setsMessageOnRecipient() {
        when(pnDataVaultClient.getInformalMessageById(any(UUID.class), any(UUID.class)))
                .thenReturn(new MessageResponseDto());

        NotificationRecipient recipient = buildRecipient(MESSAGE_ID);
        InternalNotification notification = buildNotification(SENDER_PA_ID, List.of(recipient));

        messageEnricher.enrichInternalNotification(notification);

        assertNotNull(recipient.getMessage());
        verify(pnDataVaultClient).getInformalMessageById(
                UUID.fromString(MESSAGE_ID),
                UUID.fromString(SENDER_PA_ID)
        );
    }

    @Test
    void enrichInternalNotification_withMultipleRecipients_setsMessageOnEachRecipient() {
        when(pnDataVaultClient.getInformalMessageById(any(UUID.class), any(UUID.class)))
                .thenReturn(new MessageResponseDto());

        NotificationRecipient recipient1 = buildRecipient(MESSAGE_ID);
        NotificationRecipient recipient2 = buildRecipient(ANOTHER_MESSAGE_ID);
        InternalNotification notification = buildNotification(SENDER_PA_ID, List.of(recipient1, recipient2));

        messageEnricher.enrichInternalNotification(notification);

        assertNotNull(recipient1.getMessage());
        assertNotNull(recipient2.getMessage());
        verify(pnDataVaultClient, times(2)).getInformalMessageById(any(UUID.class), any(UUID.class));
    }

    @Test
    void enrichInternalNotification_withNoRecipients_doesNotCallDataVaultClient() {
        InternalNotification notification = buildNotification(SENDER_PA_ID, Collections.emptyList());

        messageEnricher.enrichInternalNotification(notification);

        verifyNoInteractions(pnDataVaultClient);
    }

    @Test
    void enrichInternalNotification_whenDataVaultClientThrowsException_throwsPnInternalException() {
        when(pnDataVaultClient.getInformalMessageById(any(UUID.class), any(UUID.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        NotificationRecipient recipient = buildRecipient(MESSAGE_ID);
        InternalNotification notification = buildNotification(SENDER_PA_ID, List.of(recipient));

        assertThrows(PnInternalException.class, () ->
                messageEnricher.enrichInternalNotification(notification));
    }

    @Test
    void enrichInternalNotification_whenRecipientMessageIdIsNotValidUUID_throwsPnInternalException() {
        NotificationRecipient recipient = buildRecipient("not-a-valid-uuid");
        InternalNotification notification = buildNotification(SENDER_PA_ID, List.of(recipient));

        assertThrows(PnInternalException.class, () ->
                messageEnricher.enrichInternalNotification(notification));
    }

    @Test
    void enrichInternalNotification_whenSenderPaIdIsNotValidUUID_throwsPnInternalException() {
        NotificationRecipient recipient = buildRecipient(MESSAGE_ID);
        InternalNotification notification = buildNotification("not-a-valid-uuid", List.of(recipient));

        assertThrows(PnInternalException.class, () ->
                messageEnricher.enrichInternalNotification(notification));
    }

    private NotificationRecipient buildRecipient(String messageId) {
        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setMessageId(messageId);
        recipient.setInternalId("PF-" + UUID.randomUUID());
        return recipient;
    }

    private InternalNotification buildNotification(String senderPaId, List<NotificationRecipient> recipients) {
        InternalNotification notification = new InternalNotification();
        notification.setSenderPaId(senderPaId);
        notification.setRecipients(recipients);
        return notification;
    }
}