package it.pagopa.pn.delivery.pnclient.datavault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.NotificationsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;

import java.util.ArrayList;
import java.util.List;

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
}

