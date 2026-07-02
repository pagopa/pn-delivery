package it.pagopa.pn.delivery.pnclient.datavault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.delivery.exception.PnDeliveryMessageNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.MessagesApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.NotificationsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;

@ContextConfiguration(classes = {PnDataVaultClientImpl.class})
@ExtendWith(SpringExtension.class)
class PnDataVaultClientImplTest {
    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.NotificationsApi")
    private NotificationsApi notificationsApi;

    @Autowired
    private PnDataVaultClientImpl pnDataVaultClientImpl;

    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.RecipientsApi")
    private RecipientsApi recipientsApi;

    @MockBean(name = "it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.MessagesApi")
    private MessagesApi messagesApi;

    /**
     * Method under test: {@link PnDataVaultClientImpl#ensureRecipientByExternalId(RecipientType, String)}
     */
    @Test
    void testEnsureRecipientByExternalId() throws RestClientException {
        when(recipientsApi.ensureRecipientByExternalId(Mockito.<RecipientType>any(), Mockito.<String>any()))
                .thenReturn("42");
        assertEquals("42", pnDataVaultClientImpl.ensureRecipientByExternalId(RecipientType.PF, "42"));
        verify(recipientsApi).ensureRecipientByExternalId(Mockito.<RecipientType>any(), Mockito.<String>any());
    }

    /**
     * Method under test: {@link PnDataVaultClientImpl#ensureRecipientByExternalId(RecipientType, String)}
     */
    @Test
    void testEnsureRecipientByExternalId2() throws RestClientException {
        when(recipientsApi.ensureRecipientByExternalId(Mockito.<RecipientType>any(), Mockito.<String>any()))
                .thenReturn("42");
        assertEquals("42", pnDataVaultClientImpl.ensureRecipientByExternalId(RecipientType.PG, "42"));
        verify(recipientsApi).ensureRecipientByExternalId(Mockito.<RecipientType>any(), Mockito.<String>any());
    }

    /**
     * Method under test: {@link PnDataVaultClientImpl#updateNotificationAddressesByIun(String, List)}
     */
    @Test
    void testUpdateNotificationAddressesByIun() throws RestClientException {
        doNothing().when(notificationsApi)
                .updateNotificationAddressesByIun(Mockito.<String>any(), Mockito.<Boolean>any(),
                        Mockito.<List<NotificationRecipientAddressesDto>>any());
        pnDataVaultClientImpl.updateNotificationAddressesByIun("Iun", new ArrayList<>());
        verify(notificationsApi).updateNotificationAddressesByIun(Mockito.<String>any(), Mockito.<Boolean>any(),
                Mockito.<List<NotificationRecipientAddressesDto>>any());
    }

    /**
     * Method under test: {@link PnDataVaultClientImpl#getRecipientDenominationByInternalId(List)}
     */
    @Test
    void testGetRecipientDenominationByInternalId() throws RestClientException {
        ArrayList<BaseRecipientDto> baseRecipientDtoList = new ArrayList<>();
        when(recipientsApi.getRecipientDenominationByInternalId(Mockito.<List<String>>any()))
                .thenReturn(baseRecipientDtoList);
        List<BaseRecipientDto> actualRecipientDenominationByInternalId = pnDataVaultClientImpl
                .getRecipientDenominationByInternalId(new ArrayList<>());
        assertSame(baseRecipientDtoList, actualRecipientDenominationByInternalId);
        assertTrue(actualRecipientDenominationByInternalId.isEmpty());
        verify(recipientsApi).getRecipientDenominationByInternalId(Mockito.<List<String>>any());
    }

    /**
     * Method under test: {@link PnDataVaultClientImpl#getNotificationAddressesByIun(String)}
     */
    @Test
    void testGetNotificationAddressesByIun() throws RestClientException {
        ArrayList<NotificationRecipientAddressesDto> notificationRecipientAddressesDtoList = new ArrayList<>();
        when(notificationsApi.getNotificationAddressesByIun(Mockito.<String>any(), Mockito.<Boolean>any()))
                .thenReturn(notificationRecipientAddressesDtoList);
        List<NotificationRecipientAddressesDto> actualNotificationAddressesByIun = pnDataVaultClientImpl
                .getNotificationAddressesByIun("Iun");
        assertSame(notificationRecipientAddressesDtoList, actualNotificationAddressesByIun);
        assertTrue(actualNotificationAddressesByIun.isEmpty());
        verify(notificationsApi).getNotificationAddressesByIun(Mockito.<String>any(), Mockito.<Boolean>any());
    }

    /**
     * Method under test: {@link PnDataVaultClientImpl#createInformalMessage(MessageRequestDto)}
     */
    @Test
    void testCreateInformalMessage() {
        MessageRequestDto request = new MessageRequestDto();
        MessageResponseDto response = new MessageResponseDto();
        when(messagesApi.createMessage(Mockito.any())).thenReturn(response);
        MessageResponseDto result = pnDataVaultClientImpl.createInformalMessage(request);
        assertSame(response, result);
        verify(messagesApi).createMessage(Mockito.any());
    }

    /**
     * Method under test: {@link PnDataVaultClientImpl#getInformalMessageById(UUID, UUID)}
     */
    @Test
    void testGetInformalMessageById() {
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        MessageResponseDto response = new MessageResponseDto();
        when(messagesApi.getMessageById(messageId, senderId)).thenReturn(response);
        MessageResponseDto result = pnDataVaultClientImpl.getInformalMessageById(messageId, senderId);
        assertSame(response, result);
        verify(messagesApi).getMessageById(messageId, senderId);
    }

    /**
     * Method under test: {@link PnDataVaultClientImpl#getInformalMessageById(UUID, UUID)} when 404 is thrown
     */
    @Test
    void testGetInformalMessageById_Throws404Exception() {
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        PnHttpResponseException httpException = new PnHttpResponseException(
                "Not Found",
                404
        );

        when(messagesApi.getMessageById(messageId, senderId))
                .thenThrow(httpException);

        PnDeliveryMessageNotFoundException exception = org.junit.jupiter.api.Assertions.assertThrows(
                PnDeliveryMessageNotFoundException.class,
                () -> pnDataVaultClientImpl.getInformalMessageById(messageId, senderId)
        );

        assertEquals("Message does not match the senderId or was not found", exception.getMessage());
        verify(messagesApi).getMessageById(messageId, senderId);
    }

    /**
     * Method under test: {@link PnDataVaultClientImpl#getInformalMessageById(UUID, UUID)} when non-404 exception is thrown
     */
    @Test
    void testGetInformalMessageById_ThrowsGenericException() {
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        RuntimeException genericException = new RuntimeException("Generic error");

        when(messagesApi.getMessageById(messageId, senderId))
                .thenThrow(genericException);

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> pnDataVaultClientImpl.getInformalMessageById(messageId, senderId)
        );

        assertEquals("Generic error", exception.getMessage());
        verify(messagesApi).getMessageById(messageId, senderId);
    }
}
