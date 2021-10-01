package it.pagopa.pn.delivery.svc;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.svc.AttachmentService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationRetrieverService {

	private final AttachmentService attachmentService;
	private final Clock clock;
	private final NotificationViewedProducer notificationAcknowledgementProducer;
	private final NotificationDao notificationDao;
	private final TimelineDao timelineDao;
	private final StatusUtils statusUtils;

	@Autowired
	public NotificationRetrieverService(Clock clock,
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

	public List<NotificationSearchRow> searchNotification(
			boolean bySender, String senderReceiverId, Instant startDate, Instant endDate,
			String filterId, NotificationStatus status, String subjectRegExp
	) {
		return notificationDao.searchNotification(bySender, senderReceiverId, startDate, endDate, filterId, status, subjectRegExp);
	}

	/**
	 * Get the full detail of a notification by IUN
	 *
	 * @param iun unique identifier of a Notification
	 * 
	 * @return Notification DTO
	 * 
	 */
	public Notification getNotificationInformation(String iun) {
		Optional<Notification> optNotification = notificationDao.getNotificationByIun( iun );
        
        if (optNotification.isPresent() ) {
			Notification notification = optNotification.get();

			return enrichWithTimelineAndStatusHistory(iun, notification);
		} else {
        	log.debug( "Error in retrieving Notification with iun {}", iun );
			throw new PnInternalException( "Error in retrieving Notification with iun " + iun );
        }
	}

	private Notification enrichWithTimelineAndStatusHistory(String iun, Notification notification) {
		Set<TimelineElement> rawTimeline = timelineDao.getTimeline(iun);
		List<TimelineElement> timeline = rawTimeline
				.stream()
				.sorted( Comparator.comparing( TimelineElement::getTimestamp ))
				.collect(Collectors.toList());

		int numberOfRecipients = notification.getRecipients().size();
		Instant createdAt =  notification.getSentAt();
		List<NotificationStatusHistoryElement>  statusHistory = statusUtils
							 .getStatusHistory( rawTimeline, numberOfRecipients, createdAt );

		return notification
				.toBuilder()
				.timeline( timeline )
				.notificationStatusHistory( statusHistory )
				.notificationStatus( statusUtils.getCurrentStatus( statusHistory ))
				.build();
	}

	public ResponseEntity<Resource> downloadDocument(String iun, int documentIndex, String downloaderRecipientId ) {
		ResponseEntity<Resource> response;

		log.info("Retrieve notification with iun={} ", iun);
		Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

		if (optNotification.isPresent()) {
			Notification notification = optNotification.get();
			log.debug("Document download START for iun {} and documentIndex {} ", iun, documentIndex);

			NotificationAttachment doc = notification.getDocuments().get(documentIndex);
			response = attachmentService.loadAttachment( doc.getRef() );

			if (StringUtils.isNotBlank(downloaderRecipientId)) {
				notifyNotificationViewedEvent( notification, downloaderRecipientId );
			}
		} else {
			log.error("Notification not found for iun {}", iun);
			throw new PnInternalException("Notification not found for iun " + iun);
		}

		return response;
	}

	private void notifyNotificationViewedEvent(Notification notification, String userId) {
		String iun = notification.getIun();

		int recipientIndex = -1;
		for( int idx = 0 ; idx < notification.getRecipients().size(); idx++) {
			NotificationRecipient nr = notification.getRecipients().get( idx );
			if( userId.equals( nr.getTaxId() ) ) {
				recipientIndex = idx;
			}
		}

		if( recipientIndex == -1 ) {
			log.debug("Recipient not found for iun and userId{} ", iun, userId );
			throw new PnInternalException( "Notification with iun " + iun + " do not have recipient " + userId );
		}

		log.debug("Send \"notification acknowlwdgement\" event for iun {}", iun);
		Instant createdAt = clock.instant();
		notificationAcknowledgementProducer.sendNotificationViewed( iun, createdAt, recipientIndex );
	}


	public List<LegalFactsListEntry> listNotificationLegalFacts(String iun) {
		return attachmentService.listNotificationLegalFacts( iun );
	}

	public ResponseEntity<Resource> downloadLegalFact(String iun, String legalfactId) {
		return attachmentService.loadLegalfact( iun, legalfactId );
	}
}
