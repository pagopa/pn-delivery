package it.pagopa.pn.delivery.svc.sentnotification;

import java.time.Clock;
import java.util.Optional;

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

	@Autowired
	public NotificationSentService( Clock clock,
			AttachmentService attachmentService,
			NotificationViewedProducer notificationAcknowledgementProducer, NotificationDao notificationDao ) {
		this.clock = clock;
		this.attachmentService = attachmentService;
		this.notificationAcknowledgementProducer = notificationAcknowledgementProducer;
		this.notificationDao = notificationDao;
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
		log.debug( "Received Notification START {} " , iun);
		Notification notification = null;
		
		log.info( "Retrieve notification with iun={} ", iun );
        Optional<Notification> optNotification = notificationDao.getNotificationByIun( iun );
        
        if (optNotification.isPresent() ) {
        	notification = optNotification.get();
        } else {
        	log.debug( "Error in retrieving Notification with iun {}", iun );
			throw new PnInternalException( "Error in retrieving Notification with iun " + iun );
        }
        
		return notification;
	}

}
