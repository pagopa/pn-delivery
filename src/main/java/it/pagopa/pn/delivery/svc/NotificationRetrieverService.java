package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationRetrieverService {

	private final AttachmentService attachmentService;
	private final S3PresignedUrlService presignedUrlSvc;
	private final Clock clock;
	private final NotificationViewedProducer notificationAcknowledgementProducer;
	private final NotificationDao notificationDao;
	private final TimelineDao timelineDao;
	private final StatusUtils statusUtils;

	@Autowired
	public NotificationRetrieverService(Clock clock,
										AttachmentService attachmentService,
										S3PresignedUrlService presignedUrlSvc,
										NotificationViewedProducer notificationAcknowledgementProducer,
										NotificationDao notificationDao,
										TimelineDao timelineDao,
										StatusUtils statusUtils
	) {
		this.clock = clock;
		this.attachmentService = attachmentService;
		this.presignedUrlSvc = presignedUrlSvc;
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
		log.debug( "Retrieve notification by iun={} START", iun );
		Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

		if (optNotification.isPresent()) {
			Notification notification = optNotification.get();

			return enrichWithTimelineAndStatusHistory(iun, notification);
		} else {
			log.debug("Error in retrieving Notification with iun={}", iun);
			throw new PnInternalException("Error in retrieving Notification with iun " + iun);
		}
	}

	/**
	 * Get the full detail of a notification by IUN and notify viewed event
	 *
	 * @param iun    unique identifier of a Notification
	 * @param userId identifier of a user
	 * @return Notification
	 */
	public Notification getNotificationAndNotifyViewedEvent(String iun, String userId) {
		log.debug("Start getNotificationAndSetViewed for {}", iun);
		Notification notification = getNotificationInformation(iun);
		handleNotificationViewedEvent(iun, userId, notification);
		return notification;
	}

	private void handleNotificationViewedEvent(String iun, String userId, Notification notification) {
		if (StringUtils.isNotBlank(userId)) {
			notifyNotificationViewedEvent(notification, userId);
		} else {
			log.error("UserId is not present, can't update state {}", iun);
			throw new PnInternalException("UserId is not present, can't update state " + iun);
		}
	}

	private Notification enrichWithTimelineAndStatusHistory(String iun, Notification notification) {
		log.debug( "Retrieve timeline for iun={}", iun );
		Set<TimelineElement> rawTimeline = timelineDao.getTimeline(iun);
		List<TimelineElement> timeline = rawTimeline
				.stream()
				.sorted( Comparator.comparing( TimelineElement::getTimestamp ))
				.collect(Collectors.toList());

		int numberOfRecipients = notification.getRecipients().size();
		Instant createdAt =  notification.getSentAt();
		log.debug( "Retrieve status history for notification created at={}", createdAt );
		List<NotificationStatusHistoryElement>  statusHistory = statusUtils
							 .getStatusHistory( rawTimeline, numberOfRecipients, createdAt );

		return notification
				.toBuilder()
				.timeline( timeline )
				.notificationStatusHistory( statusHistory )
				.notificationStatus( statusUtils.getCurrentStatus( statusHistory ))
				.build();
	}

	public ResponseEntity<Resource> downloadDocument(String iun, int documentIndex) {
		ResponseEntity<Resource> response;

		log.info("Retrieve notification with iun={} ", iun);
		Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

		if (optNotification.isPresent()) {
			Notification notification = optNotification.get();
			log.debug("Document download START for iun {} and documentIndex {} ", iun, documentIndex);

			NotificationAttachment doc = notification.getDocuments().get(documentIndex);
			response = attachmentService.loadAttachment( doc.getRef() );
		} else {
			log.error("Notification not found for iun {}", iun);
			throw new PnInternalException("Notification not found for iun " + iun);
		}

		return response;
	}

	public String downloadDocumentWithRedirect(String iun, int documentIndex) {
		PreloadResponse response;

		log.info("Retrieve notification with iun={} ", iun);
		Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

		if (optNotification.isPresent()) {
			Notification notification = optNotification.get();
			log.debug("Document download START for iun={} and documentIndex={} ", iun, documentIndex);

			NotificationAttachment doc = notification.getDocuments().get(documentIndex);
			String fileName = iun + "doc_" + documentIndex;
			response = presignedUrlSvc.presignedDownload( fileName, doc );
		} else {
			log.error("Notification not found for iun={}", iun);
			throw new PnInternalException("Notification not found for iun=" + iun);
		}

		return response.getUrl();
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
			log.debug("Recipient not found for iun={} and userId={} ", iun, userId );
			throw new PnInternalException( "Notification with iun=" + iun + " do not have recipient=" + userId );
		}

		log.debug("Send \"notification acknowlwdgement\" event for iun={}", iun);
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
