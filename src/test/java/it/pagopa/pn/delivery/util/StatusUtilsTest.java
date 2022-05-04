package it.pagopa.pn.delivery.util;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.api.dto.notification.timeline.TimelineInfoDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class StatusUtilsTest {

    private StatusUtils statusUtils;

    @BeforeEach
    public void setup() {
        this.statusUtils = new StatusUtils();
    }

    @Test
    void emptyTimelineInitialStateTest() {
        //
        Assertions.assertEquals(NotificationStatus.IN_VALIDATION, statusUtils.getCurrentStatus(Collections.emptyList()));
    }

    @Test
    void getCurrentStatusTest() {
        TimelineInfoDto timelineElement1 = TimelineInfoDto.builder()
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .build();
        TimelineInfoDto timelineElement2 = TimelineInfoDto.builder()
                .timestamp(Instant.parse("2021-09-16T15:25:00.00Z"))
                .category(TimelineElementCategory.NOTIFICATION_PATH_CHOOSE)
                .build();
        TimelineInfoDto timelineElement3 = TimelineInfoDto.builder()
                .timestamp(Instant.parse("2021-09-16T15:26:00.00Z"))
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE)
                .build();

        Set<TimelineInfoDto> timelineElementList = Set.of(timelineElement1,
                timelineElement2, timelineElement3);
        
        //TODO Modificare
        
        //List<NotificationStatusHistoryElement> resHistoryElementList = statusUtils.getStatusHistory(
        //        timelineElementList, 1, Instant.now());

        //Assertions.assertEquals(NotificationStatus.DELIVERING, statusUtils.getCurrentStatus(resHistoryElementList));
    }

}