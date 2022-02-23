package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.ReceivedDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
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

        String id1 = "sender_ack";
        TimelineElement row1 = TimelineElement.builder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .details(new ReceivedDetails())
                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                .build();
        String id2 = "path_choose";
        TimelineElement row2 = TimelineElement.builder()
                .iun(iun)
                .elementId(id2)
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE)
                .details(new NotificationPathChooseDetails())
                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                .build();
        Set<TimelineElement> set = new HashSet<>();
        set.add(row1);
        set.add(row2);

        TimelineElement newTimelineElement = TimelineElement.builder()
                .iun(iun)
                .elementId(id2)
                .category(TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW)
                .details(new NotificationPathChooseDetails())
                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
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