package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import it.pagopa.pn.commons_delivery.middleware.notificationdao.CassandraNotificationByRecipientEntityDao;
import it.pagopa.pn.commons_delivery.middleware.notificationdao.CassandraNotificationBySenderEntityDao;
import it.pagopa.pn.commons_delivery.middleware.notificationdao.CassandraNotificationEntityDao;
import it.pagopa.pn.commons_delivery.model.notification.cassandra.NotificationEntity;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

class StatusServiceTest {
    @Mock
    private CassandraNotificationEntityDao notificationEntityDao;
    @Mock
    private CassandraNotificationBySenderEntityDao notificationBySenderEntityDao;
    @Mock
    private CassandraNotificationByRecipientEntityDao notificationByRecipientEntityDao;

    private StatusUtils statusUtils = new StatusUtils();

    private StatusService statusService;

    @BeforeEach
    public void setup() {
        statusService = new StatusService(notificationEntityDao, statusUtils, notificationBySenderEntityDao, notificationByRecipientEntityDao);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void updateStatus() {

        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        // WHEN
        Optional<NotificationEntity> notification = Optional.ofNullable(NotificationEntity.builder()
                .iun(iun)
                .recipientsOrder(Collections.singletonList("CodiceFiscale"))
                .build());
        Mockito.when(notificationEntityDao.get(iun)).thenReturn(notification);

        TimelineInfoDto row1 = TimelineInfoDto.builder()
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .timestamp(Instant.parse("2021-09-16T15:28:00.00Z"))
                .build();
        TimelineInfoDto row2 = TimelineInfoDto.builder()
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE)
                .timestamp(Instant.parse("2021-09-16T16:28:00.00Z"))
                .build();
        Set<TimelineInfoDto> set = new HashSet<>();
        set.add(row1);
        set.add(row2);

        TimelineInfoDto newTimelineElement = TimelineInfoDto.builder()
                .category(TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW)
                .timestamp(Instant.parse("2021-09-16T17:28:00.00Z"))
                .build();
        
        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(iun)
                .currentTimeline(set)
                .newTimelineElement(newTimelineElement)
                .build();

        ResponseUpdateStatusDto responseDto = statusService.updateStatus(dto);

        Assertions.assertEquals(NotificationStatus.DELIVERING, responseDto.getCurrentStatus());
        Assertions.assertEquals(NotificationStatus.DELIVERED, responseDto.getNextStatus());
    }
}
