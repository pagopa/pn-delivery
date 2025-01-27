package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV23;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
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
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class StatusServiceTest {

    @Mock
    private NotificationDao notificationDao;
    @Mock
    private NotificationMetadataEntityDao notificationMetadataEntityDao;
    @Mock
    private NotificationDelegationMetadataEntityDao notificationDelegationMetadataEntityDao;
    @Mock
    private NotificationDelegatedService notificationDelegatedService;
    @Mock
    private PnExternalRegistriesClientImpl externalRegistriesClient;
    
    private StatusService statusService;
    @Mock
    private NotificationCostEntityDao notificationCostEntityDao;

    @BeforeEach
    public void setup() {
        statusService = new StatusService(notificationDao, notificationMetadataEntityDao, notificationDelegationMetadataEntityDao, notificationDelegatedService, notificationCostEntityDao, externalRegistriesClient);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatus() {

        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        // WHEN
        Optional<InternalNotification> notification = Optional.of(newInternalNotification(iun, NotificationStatusV26.ACCEPTED));
        when(notificationDao.getNotificationByIun(iun, false)).thenReturn(notification);
        when(notificationMetadataEntityDao.get( Mockito.any(Key.class) )).thenReturn( Optional.of( NotificationMetadataEntity.builder()
                        .tableRow( Map.of( "acceptedAt", "2021-09-16T16:00Z") )
                .build() )
        );
        
        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextStatus(NotificationStatusV26.DELIVERED)
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
                .nextStatus(NotificationStatusV26.DELIVERED)
                .timestamp( OffsetDateTime.now() )
                .build();

        when(notificationDao.getNotificationByIun(iun, false)).thenReturn(Optional.empty());

        Executable todo = () -> statusService.updateStatus( dto );

        Assertions.assertThrows(PnInternalException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatus_Notification_REFUSED_With_Payments() {

        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";
        NotificationPaymentInfo notificationPaymentInfo = NotificationPaymentInfo.builder()
                .pagoPa(PagoPaPayment.builder()
                        .creditorTaxId("creditorTaxId")
                        .noticeCode("noticeCode")
                        .build())
                .build();
        notificationPaymentInfo.setPagoPa(PagoPaPayment.builder()
                .creditorTaxId("creditorTaxId")
                .noticeCode("noticeCode")
                .build());
        NotificationRecipient notificationRecipient= new NotificationRecipient();
        notificationRecipient.setPayment(List.of(notificationPaymentInfo));


        // WHEN
        Optional<InternalNotification> notification = Optional.of(newInternalNotification(iun, NotificationStatusV26.IN_VALIDATION));
        when(notificationDao.getNotificationByIun(iun, false)).thenReturn(notification);
        notification.get().setRecipients(List.of(notificationRecipient));

        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextStatus(NotificationStatusV26.REFUSED)
                .timestamp( OffsetDateTime.now() )
                .build();
        assertDoesNotThrow(() -> statusService.updateStatus(dto));
        Mockito.verify(notificationCostEntityDao, times(1)).deleteItem(Mockito.any());
        Mockito.verify(notificationMetadataEntityDao, times(0)).get(Mockito.any());
        Mockito.verify(notificationDelegatedService, times(0)).computeDelegationMetadataEntries(Mockito.any(NotificationMetadataEntity.class));
        Mockito.verify(notificationMetadataEntityDao, times(0)).put(Mockito.any(NotificationMetadataEntity.class));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatus_Notification_REFUSED_Without_Payments() {

        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        // WHEN
        Optional<InternalNotification> notification = Optional.of(newInternalNotification(iun, NotificationStatus.IN_VALIDATION));

        when(notificationDao.getNotificationByIun(iun, false)).thenReturn(notification);

        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextStatus(NotificationStatus.REFUSED)
                .timestamp( OffsetDateTime.now() )
                .build();
        assertDoesNotThrow(() -> statusService.updateStatus(dto));
        Mockito.verify(notificationCostEntityDao, times(0)).deleteItem(Mockito.any());
        Mockito.verify(notificationMetadataEntityDao, times(0)).get(Mockito.any());
        Mockito.verify(notificationDelegatedService, times(0)).computeDelegationMetadataEntries(Mockito.any(NotificationMetadataEntity.class));
        Mockito.verify(notificationMetadataEntityDao, times(0)).put(Mockito.any(NotificationMetadataEntity.class));
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatusDefault() {
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";
        Optional<InternalNotification> Internalnotification = Optional.of(newInternalNotification(iun, NotificationStatusV26.IN_VALIDATION));
        when(notificationDao.getNotificationByIun(iun, false)).thenReturn(Internalnotification);

        // WHEN
        Optional<NotificationMetadataEntity> notification = Optional.of(NotificationMetadataEntity.builder()
                .tableRow(Map.of("acceptedAt", "2021-09-16T16:00Z"))
                .build());
        when(notificationMetadataEntityDao.get(any(Key.class))).thenReturn(notification);

        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextStatus(NotificationStatusV26.DELIVERED)
                .timestamp( OffsetDateTime.now() )
                .build();
        assertDoesNotThrow(() -> statusService.updateStatus(dto));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatusDefaultWithAnOldStatus() {
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";
        Optional<InternalNotification> Internalnotification = Optional.of(newInternalNotification(iun, NotificationStatus.IN_VALIDATION));
        when(notificationDao.getNotificationByIun(iun, false)).thenReturn(Internalnotification);

        Instant now = OffsetDateTime.now().toInstant();
        OffsetDateTime dayBefore = OffsetDateTime.now().minusDays(1);

        // WHEN
        Optional<NotificationMetadataEntity> notificationMetadata = Optional.of(NotificationMetadataEntity.builder()
                .tableRow(Map.of("acceptedAt", "2021-09-16T16:00Z"))
                .notificationStatusTimestamp(now)
                .build());
        when(notificationMetadataEntityDao.get(any(Key.class))).thenReturn(notificationMetadata);

        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextStatus(NotificationStatus.DELIVERED)
                .timestamp( dayBefore )
                .build();
        assertDoesNotThrow(() -> statusService.updateStatus(dto));

        // Non avvengono inserimenti in tabella
        verify(notificationMetadataEntityDao, times(0)).put(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatusOptionalEmpty() {
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";
        NotificationPaymentInfo notificationPaymentInfo = NotificationPaymentInfo.builder()
                .pagoPa(PagoPaPayment.builder()
                        .creditorTaxId("creditorTaxId")
                        .noticeCode("noticeCode")
                        .build())
                .build();
        notificationPaymentInfo.setPagoPa(PagoPaPayment.builder()
                .creditorTaxId("creditorTaxId")
                .noticeCode("noticeCode")
                .build());
        NotificationRecipient notificationRecipient= new NotificationRecipient();
        notificationRecipient.setPayment(List.of(notificationPaymentInfo));

        Optional<InternalNotification> internalNotification = Optional.of(newInternalNotification(iun, NotificationStatusV26.IN_VALIDATION));
        when(notificationDao.getNotificationByIun(iun, false)).thenReturn(internalNotification);
        internalNotification.get().setRecipients(List.of(notificationRecipient));
        internalNotification.get().setRecipientIds(List.of("recipientId"));
        // WHEN
        Optional<NotificationMetadataEntity> notification = Optional.empty();
        when(notificationMetadataEntityDao.get(any(Key.class))).thenReturn(notification);

        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextStatus(NotificationStatusV26.DELIVERED)
                .timestamp( OffsetDateTime.now() )
                .build();
        assertDoesNotThrow(() -> statusService.updateStatus(dto));
    }

    @NotNull
    private static InternalNotification newInternalNotification(String iun, NotificationStatusV26 inValidation) {
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setRecipients(
                List.of(
                        NotificationRecipient.builder()
                                .internalId("internalId")
                                .recipientType(NotificationRecipientV23.RecipientTypeEnum.PG)
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
        internalNotification.setRecipientIds(List.of("internalId"));
        return internalNotification;
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatus_Notification_ACCEPTED() {
        RequestUpdateStatusDto requestUpdateStatusDto = RequestUpdateStatusDto.builder()
                .iun( "FAKE_IUN" )
                .nextStatus( NotificationStatusV26.ACCEPTED )
                .timestamp( OffsetDateTime.parse( "2023-04-24T12:15:23Z" ) )
                .build();

        InternalNotification notification = newInternalNotification( "FAKE_IUN", NotificationStatusV26.IN_VALIDATION );

        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() ) ).thenReturn( Optional.of( notification ) );

        assertDoesNotThrow(() -> statusService.updateStatus( requestUpdateStatusDto ) );
        Mockito.verify(notificationMetadataEntityDao, times(1)).put(Mockito.any());
    }
}
