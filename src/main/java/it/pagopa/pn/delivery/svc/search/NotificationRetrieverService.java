package it.pagopa.pn.delivery.svc.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineInfoDto;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClient;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.svc.S3PresignedUrlService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
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

	private final FileStorage fileStorage;
	private final S3PresignedUrlService presignedUrlSvc;
	private final Clock clock;
	private final NotificationViewedProducer notificationAcknowledgementProducer;
	private final NotificationDao notificationDao;
	private final PnDeliveryPushClient pnDeliveryPushClient;
	private final StatusUtils statusUtils;
	private final PnDeliveryConfigs cfg;
	private final PnMandateClientImpl pnMandateClient;


	@Autowired
	public NotificationRetrieverService(Clock clock,
										FileStorage fileStorage,
										S3PresignedUrlService presignedUrlSvc,
										NotificationViewedProducer notificationAcknowledgementProducer,
										NotificationDao notificationDao,
										PnDeliveryPushClient pnDeliveryPushClient,
										StatusUtils statusUtils,
										PnDeliveryConfigs cfg,
										PnMandateClientImpl pnMandateClient) {
		this.fileStorage = fileStorage;
		this.clock = clock;
		this.presignedUrlSvc = presignedUrlSvc;
		this.notificationAcknowledgementProducer = notificationAcknowledgementProducer;
		this.notificationDao = notificationDao;
		this.pnDeliveryPushClient = pnDeliveryPushClient;
		this.statusUtils = statusUtils;
		this.cfg = cfg;
		this.pnMandateClient = pnMandateClient;
	}

	public ResultPaginationDto<NotificationSearchRow,String> searchNotification( InputSearchNotificationDto searchDto, String uid ) {
		log.info("Start search notification - senderReceiverId={}", searchDto.getSenderReceiverId());

		validateInput(searchDto);
		String senderReceiverId = searchDto.getSenderReceiverId();
		if (!uid.equals(senderReceiverId)) {
			log.info( "Start check mandate for uid={}", uid );
			checkMandate(searchDto, uid);
		}

		PnLastEvaluatedKey lastEvaluatedKey = null;
		if ( searchDto.getNextPagesKey() != null ) {
			try {
				lastEvaluatedKey = PnLastEvaluatedKey.deserializeInternalLastEvaluatedKey( searchDto.getNextPagesKey() );
			} catch (JsonProcessingException e) {
				throw new PnInternalException( "Unable to deserialize lastEvaluatedKey", e );
			}
		}

		MultiPageSearch multiPageSearch = new MultiPageSearch(
				notificationDao,
				searchDto,
				lastEvaluatedKey,
				cfg
		);

		ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchResult = multiPageSearch.searchNotificationMetadata();

		ResultPaginationDto.ResultPaginationDtoBuilder<NotificationSearchRow,String> builder = ResultPaginationDto.builder();
		builder.moreResult( searchResult.getNextPagesKey() != null )
				.result( searchResult.getResult() );
		if ( searchResult.getNextPagesKey() != null ) {
			builder.nextPagesKey( searchResult.getNextPagesKey()
					.stream().map(PnLastEvaluatedKey::serializeInternalLastEvaluatedKey)
					.collect(Collectors.toList()) );
		}
		return builder.build();
	}

	private void checkMandate(InputSearchNotificationDto searchDto, String uid) {
		String senderReceiverId = searchDto.getSenderReceiverId();
		List<InternalMandateDto> mandates = this.pnMandateClient.getMandates(uid);
		if(!mandates.isEmpty()) {
			boolean validMandate = false;
			for ( InternalMandateDto mandate : mandates ) {
				if (mandate.getDelegator() != null && mandate.getDatefrom() != null && mandate.getDelegator().equals(senderReceiverId)) {
					Instant mandateStartDate = Instant.parse(mandate.getDatefrom());
					Instant mandateEndDate = mandate.getDateto() != null ? Instant.parse(mandate.getDateto()) : null;
					adjustSearchDates( searchDto, mandateStartDate, mandateEndDate );
					validMandate = true;
					log.info( "Valid mandate for uid={}", uid );
					break;
				}
			}
			if (!validMandate){
				String message = String.format("Unable to find valid mandate for uid=%s and cx-id=%s", uid, senderReceiverId);
				log.error( message );
				throw new PnNotFoundException( message );
			}
		} else {
			String message = String.format("Unable to find any mandate for uid=%s", uid);
			log.error( message );
			throw new PnNotFoundException( message );
		}
	}

	private void adjustSearchDates(InputSearchNotificationDto searchDto,
								   Instant mandateStartDate,
								   Instant mandateEndDate) {
		Instant searchStartDate = searchDto.getStartDate();
		Instant searchEndDate = searchDto.getEndDate();
		searchDto.setStartDate( searchStartDate.isBefore(mandateStartDate)? mandateStartDate : searchStartDate );
		if (mandateEndDate != null) {
			searchDto.setEndDate( searchEndDate.isBefore(mandateEndDate) ? searchEndDate : mandateEndDate );
		}
		log.debug( "Adjust search date, startDate{} endDate{}", searchDto.getStartDate(), searchDto.getEndDate() );
	}

	private void validateInput(InputSearchNotificationDto searchDto) {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();

		Set<ConstraintViolation<InputSearchNotificationDto>> errors = validator.validate(searchDto);
		if( ! errors.isEmpty() ) {
			log.error("Validation search input ERROR {} - senderReceiverId {}",errors, searchDto.getSenderReceiverId());
			throw new PnValidationException(searchDto.getSenderReceiverId(), errors);
		}

		log.debug("Validation search input OK - senderReceiverId {}",searchDto.getSenderReceiverId());
	}

	/**
	 * Get the full detail of a notification by IUN
	 *
	 * @param iun unique identifier of a Notification
	 *
	 * @return Notification DTO
	 *
	 */
	public Notification getNotificationInformation(String iun, boolean withTimeline) {
		log.debug( "Retrieve notification by iun={} withTimeline={} START", iun, withTimeline );
		Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

		if (optNotification.isPresent()) {
			Notification notification = optNotification.get();
			if (withTimeline) {
				notification = enrichWithTimelineAndStatusHistory(iun, notification);
			}
			return notification;
		} else {
			String msg = String.format( "Error retrieving Notification with iun=%s withTimeline=%b", iun, withTimeline );
			log.debug( msg );
			throw new PnInternalException( msg );
		}
	}

	public Notification getNotificationInformation(String iun) {
		return getNotificationInformation( iun, true );
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
		Set<TimelineElement> rawTimeline = pnDeliveryPushClient.getTimelineElements(iun);
		List<TimelineElement> timeline = rawTimeline
				.stream()
				.sorted( Comparator.comparing( TimelineElement::getTimestamp ))
				.collect(Collectors.toList());

		int numberOfRecipients = notification.getRecipients().size();
		Instant createdAt =  notification.getSentAt();
		log.debug( "Retrieve status history for notification created at={}", createdAt );

		Set<TimelineInfoDto> timelineInfoDto = rawTimeline.stream().map(elem ->
				TimelineInfoDto.builder()
						.elementId(elem.getElementId())
						.category(elem.getCategory())
						.timestamp(elem.getTimestamp())
						.build()
		).collect(Collectors.toSet());

		List<NotificationStatusHistoryElement>  statusHistory = statusUtils
				.getStatusHistory( timelineInfoDto, numberOfRecipients, createdAt );

		return notification
				.toBuilder()
				.timeline( timeline )
				.notificationStatusHistory( statusHistory )
				.notificationStatus( statusUtils.getCurrentStatus( statusHistory ))
				.build();
	}

	public ResponseEntity<Resource> downloadDocument(String iun, int documentIndex) {
		ResponseEntity<Resource> response;

		log.info("Retrieve notification with iun={} for direct download", iun);
		Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

		if (optNotification.isPresent()) {
			Notification notification = optNotification.get();
			log.debug("Document download START for iun {} and documentIndex {} ", iun, documentIndex);

			NotificationAttachment doc = notification.getDocuments().get(documentIndex);
			response = fileStorage.loadAttachment( doc.getRef() );
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

		log.info("Send \"notification acknowlwdgement\" event for iun={}", iun);
		Instant createdAt = clock.instant();
		notificationAcknowledgementProducer.sendNotificationViewed( iun, createdAt, recipientIndex );
	}
}
