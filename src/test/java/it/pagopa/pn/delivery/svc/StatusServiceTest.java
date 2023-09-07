package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV21;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationDelegationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationCostEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;

class StatusServiceTest {

    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";

    @Mock
    private NotificationDao notificationDao;
    @Mock
    private NotificationMetadataEntityDao notificationMetadataEntityDao;
    @Mock
    private NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao;
    @Mock
    private NotificationDelegatedService notificationDelegatedService;
    @Mock
    private PnDataVaultClientImpl dataVaultClient;
    
    private StatusService statusService;
    @Mock
    private NotificationCostEntityDao notificationCostEntityDao;

    @BeforeEach
    public void setup() {
        statusService = new StatusService(notificationDao, notificationMetadataEntityDao, notificationDelegationMetadataEntityDao, notificationDelegatedService, dataVaultClient, notificationCostEntityDao);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    @Disabled
    void updateStatus() {

        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        Key key = Key.builder()
                .partitionValue( iun + "##" + "recipientInternalId" )
                .sortValue( "2021-09-16T15:00Z" )
                .build();

        // WHEN
        Optional<InternalNotification> notification = Optional.of(newInternalNotification(iun, NotificationStatus.ACCEPTED));
        Mockito.when(notificationDao.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when( dataVaultClient.ensureRecipientByExternalId( RecipientType.PF, "CodiceFiscale" ) )
                .thenReturn( "CodiceFiscale" );
        Mockito.when(notificationMetadataEntityDao.get( key )).thenReturn( Optional.of( NotificationMetadataEntity.builder()
                        .tableRow( Map.of( "acceptedAt", "2021-09-16T16:00Z") )
                .build() )
        );
        
        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextStatus(NotificationStatus.DELIVERED)
                .timestamp( OffsetDateTime.now() )
                .build();
        
        assertDoesNotThrow(() -> statusService.updateStatus(dto));
        
        Mockito.verify(notificationMetadataEntityDao).put(Mockito.any(NotificationMetadataEntity.class));
        Mockito.verify(notificationCostEntityDao, times(0)).deleteItem(Mockito.any(NotificationCostEntity.class));
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

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatus_Notification_REFUSED() {

        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        // WHEN
        Optional<InternalNotification> notification = Optional.of(newInternalNotification(iun, NotificationStatus.IN_VALIDATION));
        Mockito.when(notificationDao.getNotificationByIun(iun)).thenReturn(notification);

        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextStatus(NotificationStatus.REFUSED)
                .timestamp( OffsetDateTime.now() )
                .build();

        assertDoesNotThrow(() -> statusService.updateStatus(dto));

        //Mockito.verify(notificationCostEntityDao, times(2)).deleteItem(Mockito.any(NotificationCostEntity.class));
        Mockito.verify(notificationMetadataEntityDao, times(0)).get(Mockito.any());
        Mockito.verify(notificationDelegatedService, times(0)).computeDelegationMetadataEntries(Mockito.any(NotificationMetadataEntity.class));
        Mockito.verify(notificationMetadataEntityDao, times(0)).put(Mockito.any(NotificationMetadataEntity.class));

    }

    @NotNull
    private static InternalNotification newInternalNotification(String iun, NotificationStatus inValidation) {
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setRecipients(
                List.of(
                        NotificationRecipient.builder()
                                .internalId("internalId")
                                .recipientType(NotificationRecipientV21.RecipientTypeEnum.PG)
                                .taxId("taxId")
                                .physicalAddress(it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress.builder().build())
                                .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder().build())
                                .payments(List.of(NotificationPaymentInfo.builder().build()))
                                .build()));
        internalNotification.setIun(iun);
        internalNotification.setPaProtocolNumber("protocol_01");
        internalNotification.setSubject("Subject 01");
        internalNotification.setCancelledIun("IUN_05");
        internalNotification.setCancelledIun("IUN_00");
        internalNotification.setSenderPaId("PA_ID");
        internalNotification.setNotificationStatus(inValidation);
        return internalNotification;
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatus_Notification_ACCEPTED() {
        RequestUpdateStatusDto requestUpdateStatusDto = RequestUpdateStatusDto.builder()
                .iun( "FAKE_IUN" )
                .nextStatus( NotificationStatus.ACCEPTED )
                .timestamp( OffsetDateTime.parse( "2023-04-24T12:15:23Z" ) )
                .build();

        InternalNotification notification = newInternalNotification( "FAKE_IUN", NotificationStatus.IN_VALIDATION );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) ).thenReturn( Optional.of( notification ) );

        assertDoesNotThrow(() -> statusService.updateStatus( requestUpdateStatusDto ) );
        Mockito.verify(notificationMetadataEntityDao, times(1)).put(Mockito.any());
    }
}
