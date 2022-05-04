package it.pagopa.pn.delivery.util;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class StatusUtils {

    private static final NotificationStatus INITIAL_STATUS = NotificationStatus.IN_VALIDATION;
    
    public NotificationStatus getCurrentStatus(List<NotificationStatusHistoryElement> statusHistory) {
        if (!statusHistory.isEmpty()) {
            return statusHistory.get(statusHistory.size() - 1).getStatus();
        } else {
            return INITIAL_STATUS;
        }
    }
}
