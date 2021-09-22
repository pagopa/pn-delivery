package it.pagopa.pn.delivery.svc.sentnotification;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.svc.receivenotification.AttachmentService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationSentService {

	private final AttachmentService attachmentService;
	private final Clock clock;
	private final NotificationViewedProducer notificationAcknowledgementProducer;
	private final NotificationDao notificationDao;
	private final TimelineDao timelineDao;
	private final StatusUtils statusUtils;

	@Autowired
	public NotificationSentService(Clock clock,
								   AttachmentService attachmentService,
								   NotificationViewedProducer notificationAcknowledgementProducer,
								   NotificationDao notificationDao,
								   TimelineDao timelineDao,
								   StatusUtils statusUtils
	) {
		this.clock = clock;
		this.attachmentService = attachmentService;
		this.notificationAcknowledgementProducer = notificationAcknowledgementProducer;
		this.notificationDao = notificationDao;
		this.timelineDao = timelineDao;
		this.statusUtils = statusUtils;
	}

	/**
	 * Get the full detail of a notification by IUN
	 *
	 * @param iun unique identifier of a Notification
	 * 
	 * @return Notification DTO
	 * 
	 */
	public Notification getSentNotification(String iun) {
		Optional<Notification> optNotification = notificationDao.getNotificationByIun( iun );
        
        if (optNotification.isPresent() ) {
			Set<TimelineElement> rawTimeline = timelineDao.getTimeline(iun);
			List<TimelineElement> timeline = rawTimeline
					.stream()
					.sorted( Comparator.comparing( TimelineElement::getTimestamp ))
					.collect(Collectors.toList());

			int numberOfRecipients = optNotification.get().getRecipients().size();
			Instant createdAt =  optNotification.get().getSentAt();
			List<NotificationStatusHistoryElement>  statusHistory = statusUtils
					             .getStatusHistory( rawTimeline, numberOfRecipients, createdAt );

			return optNotification.get()
					.toBuilder()
					.timeline( timeline )
					.notificationStatusHistory( statusHistory )
					.notificationStatus( statusUtils.getCurrentStatus( statusHistory ))
					.build();
        } else {
        	log.debug( "Error in retrieving Notification with iun {}", iun );
			throw new PnInternalException( "Error in retrieving Notification with iun " + iun );
        }
	}

}
