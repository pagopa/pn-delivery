package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.util.StatusUtils;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StatusServiceTest {
    @Mock
    private NotificationDao notificationDao;

    @Mock
    private NotificationMetadataEntityDao notificationMetadataEntityDao;
    
    private StatusService statusService;

    @BeforeEach
    public void setup() {
        statusService = new StatusService(notificationDao, notificationMetadataEntityDao);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatus() {

        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        // WHEN
        Optional<Notification> notification = Optional.ofNullable(Notification.builder()
                .iun(iun)
                .sentAt( Instant.parse("2021-09-16T15:00:00.00Z") )
                .subject( "Subject" )
                .paNotificationId("123")
                .sender(NotificationSender.builder()
                        .paId( "PAID" )
                        .build())
                .paNotificationId("PAID")
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .taxId( "CodiceFiscale" )
                        .build()) )
                .build());
        Mockito.when(notificationDao.getNotificationByIun(iun)).thenReturn(notification);
        
        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .nextState(NotificationStatus.DELIVERED)
                .build();
        
        assertDoesNotThrow(() -> statusService.updateStatus(dto));
        
        Mockito.verify(notificationMetadataEntityDao).put(Mockito.any(NotificationMetadataEntity.class));
    }
}
