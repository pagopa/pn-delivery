package it.pagopa.pn.delivery.util;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.api.dto.notification.timeline.TimelineInfoDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
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
        List<NotificationStatusHistoryElement>  statusHistory = new ArrayList<>();
        NotificationStatusHistoryElement statusHistoryDelivering = NotificationStatusHistoryElement.builder()
                .activeFrom(Instant.now())
                .status(NotificationStatus.DELIVERING)
                .build();
        NotificationStatusHistoryElement statusHistoryAccepted = NotificationStatusHistoryElement.builder()
                .activeFrom(Instant.now())
                .status(NotificationStatus.ACCEPTED)
                .build();
        statusHistory.add(statusHistoryDelivering);
        statusHistory.add(statusHistoryAccepted);
        
        Assertions.assertEquals(NotificationStatus.ACCEPTED, statusUtils.getCurrentStatus(statusHistory));
    }

}