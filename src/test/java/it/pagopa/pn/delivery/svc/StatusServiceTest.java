package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.function.Executable;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StatusServiceTest {
    @Mock
    private NotificationDao notificationDao;

    @Mock
    private NotificationMetadataEntityDao notificationMetadataEntityDao;

    @Mock
    private PnDataVaultClientImpl dataVaultClient;
    
    private StatusService statusService;

    @BeforeEach
    public void setup() {
        statusService = new StatusService(notificationDao, notificationMetadataEntityDao, dataVaultClient);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatus() {

        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        // WHEN
        Optional<InternalNotification> notification = Optional.of(new InternalNotification(FullSentNotification.builder()
                .iun(iun)
                .sentAt( OffsetDateTime.parse("2021-09-16T15:00:00.00Z") )
                .subject( "Subject" )
                .paProtocolNumber( "123" )
                .senderPaId( "PAID" )
                .senderDenomination( "senderDenomination" )
                .notificationStatus( NotificationStatus.ACCEPTED )
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .taxId( "CodiceFiscale" )
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .build()) )
                .build(), Collections.emptyMap(), List.of( "recipientId" )));
        Mockito.when(notificationDao.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when( dataVaultClient.ensureRecipientByExternalId( RecipientType.PF, "CodiceFiscale" ) )
                .thenReturn( "CodiceFiscale" );
        
        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextStatus(NotificationStatus.DELIVERED)
                .timestamp( OffsetDateTime.now() )
                .build();
        
        assertDoesNotThrow(() -> statusService.updateStatus(dto));
        
        Mockito.verify(notificationMetadataEntityDao).put(Mockito.any(NotificationMetadataEntity.class));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatusKo() {
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";
        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextStatus(NotificationStatus.DELIVERED)
                .timestamp( OffsetDateTime.now() )
                .build();

        Mockito.when(notificationDao.getNotificationByIun(iun)).thenReturn(Optional.empty());

        Executable todo = () -> statusService.updateStatus( dto );

        Assertions.assertThrows(PnInternalException.class, todo);
    }
}
