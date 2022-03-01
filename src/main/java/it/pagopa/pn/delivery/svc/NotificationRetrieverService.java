package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
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

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationRetrieverService {

	private final FileStorage fileStorage;
	private final S3PresignedUrlService presignedUrlSvc;
	private final Clock clock;
	private final NotificationViewedProducer notificationAcknowledgementProducer;
	private final NotificationDao notificationDao;
	private final TimelineDao timelineDao;
	private final StatusUtils statusUtils;

	static final int MAX_KEY_TO_RETURN = 4;

	@Autowired
	public NotificationRetrieverService(Clock clock,
										FileStorage fileStorage,
										S3PresignedUrlService presignedUrlSvc,
										NotificationViewedProducer notificationAcknowledgementProducer,
										NotificationDao notificationDao,
										TimelineDao timelineDao,
										StatusUtils statusUtils
	) {
		this.fileStorage = fileStorage;
		this.clock = clock;
		this.presignedUrlSvc = presignedUrlSvc;
		this.notificationAcknowledgementProducer = notificationAcknowledgementProducer;
		this.notificationDao = notificationDao;
		this.timelineDao = timelineDao;
		this.statusUtils = statusUtils;
	}
	
	//TODO Questo è un Workaround. Logica da cambiare quando si passerà a DYNAMODB
	
	public ResultPaginationDto<NotificationSearchRow> searchNotification( InputSearchNotificationDto searchDto ) {
		log.debug("Start search notification - senderReceiverId {}", searchDto.getSenderReceiverId());
		
		validateInput(searchDto);
		
		List<NotificationSearchRow> rows = getNotificationSearchRows(searchDto);

		return getPaginationResult(searchDto.getSize(), rows);
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
	
	private List<NotificationSearchRow> getNotificationSearchRows(InputSearchNotificationDto searchDto) {
		Instant dateToFilter = null;

		//Verifica presenza nextPageKey, che sta ad indicare la chiave per la prossima pagina della paginazione (in questo caso è stata utilizzata la data)
		if(searchDto.getNextPagesKey() != null){
			dateToFilter = Instant.parse(searchDto.getNextPagesKey());
		}else {
			dateToFilter = searchDto.getStartDate();
		}
		searchDto.setStartDate(dateToFilter);
		log.debug("dateToFilter is {}",dateToFilter);

		return notificationDao.searchNotification(searchDto);
	}

	private ResultPaginationDto<NotificationSearchRow> getPaginationResult(Integer pageSize, List<NotificationSearchRow> rows) {
		ResultPaginationDto <NotificationSearchRow> result = ResultPaginationDto.<NotificationSearchRow>builder()
				.result(rows)
				.moreResult(false)
				.nextPagesKey(null)
				.build();
		
		boolean isLastSlice = rows == null || rows.isEmpty() || rows.size() <= pageSize;
		
		//Se il numero di risultati ottenuti è > della size della singola pagina ...
		if(! isLastSlice){
			//... viene ottenuto lo slice della lista per essere restituito ...
			List<NotificationSearchRow> subListToReturn = rows.subList(0, pageSize);
			//... viene restituita la lista delle successive chiavi per la navigazione
			result = getListNextPagesKey(pageSize, rows, subListToReturn);
		}
		return result;
	}

	private ResultPaginationDto <NotificationSearchRow> getListNextPagesKey(
			Integer size, List<NotificationSearchRow> completeResultList, List<NotificationSearchRow> subListToReturn){
		List<String> listNextPagesKey = new ArrayList<>();
		int index = 1;
		int nextSize = size * index;

		while(completeResultList.size() > nextSize && index < MAX_KEY_TO_RETURN){
			//Vengono ottenute le key dei prossimi elementi
			NotificationSearchRow firstElementNextPage = completeResultList.get(nextSize);
			listNextPagesKey.add(firstElementNextPage.getSentAt() != null ?  firstElementNextPage.getSentAt().toString() : null);
			index++;
			nextSize = size * index;
		}
		
		return ResultPaginationDto.<NotificationSearchRow>builder()
				.result(subListToReturn)
				.nextPagesKey(listNextPagesKey)
				.moreResult(completeResultList.size() > nextSize)
				.build();
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
