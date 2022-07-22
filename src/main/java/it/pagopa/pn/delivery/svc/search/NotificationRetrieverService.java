package it.pagopa.pn.delivery.svc.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationRetrieverService {

	public static final long MAX_DOCUMENTS_AVAILABLE_DAYS = 120L;
	public static final long MAX_FIRST_NOTICE_CODE_DAYS = 5L;
	public static final long MAX_SECOND_NOTICE_CODE_DAYS = 60L;

	private static final Instant PN_EPOCH = Instant.ofEpochSecond( 1651399200 ); // 2022-05-01T12:00:00.000 GMT+2:00

	private final Clock clock;
	private final NotificationViewedProducer notificationAcknowledgementProducer;
	private final NotificationDao notificationDao;
	private final PnDeliveryPushClientImpl pnDeliveryPushClient;
	private final PnMandateClientImpl pnMandateClient;
	private final PnDataVaultClientImpl dataVaultClient;
	private final ModelMapperFactory modelMapperFactory;
	private final NotificationSearchFactory notificationSearchFactory;


	@Autowired
	public NotificationRetrieverService(Clock clock,
										NotificationViewedProducer notificationAcknowledgementProducer,
										NotificationDao notificationDao,
										PnDeliveryPushClientImpl pnDeliveryPushClient,
										PnMandateClientImpl pnMandateClient, PnDataVaultClientImpl dataVaultClient, ModelMapperFactory modelMapperFactory, NotificationSearchFactory notificationSearchFactory) {
		this.clock = clock;
		this.notificationAcknowledgementProducer = notificationAcknowledgementProducer;
		this.notificationDao = notificationDao;
		this.pnDeliveryPushClient = pnDeliveryPushClient;
		this.pnMandateClient = pnMandateClient;
		this.dataVaultClient = dataVaultClient;
		this.modelMapperFactory = modelMapperFactory;
		this.notificationSearchFactory = notificationSearchFactory;
	}

	public ResultPaginationDto<NotificationSearchRow,String> searchNotification(InputSearchNotificationDto searchDto ) {

		Instant startDate = searchDto.getStartDate();
		if( PN_EPOCH.isAfter(startDate) ) {
			log.info("Start date is {} but PiattaformaNotifica exsists since {} ", startDate, PN_EPOCH);
			searchDto.setStartDate( PN_EPOCH );
		}

		log.info("Start search notification - senderReceiverId={}", searchDto.getSenderReceiverId());

		validateInput(searchDto);

		if ( !searchDto.isBySender() ) {
			String mandateId = searchDto.getMandateId();
			if ( StringUtils.hasText( mandateId )) {
				checkMandate(searchDto, mandateId);
			} else {
				log.debug( "Search from receiver without mandate" );
			}
		} else {
			log.debug( "Search from receiver" );
		}

		PnLastEvaluatedKey lastEvaluatedKey = null;
		if ( searchDto.getNextPagesKey() != null ) {
			try {
				lastEvaluatedKey = PnLastEvaluatedKey.deserializeInternalLastEvaluatedKey( searchDto.getNextPagesKey() );
			} catch (JsonProcessingException e) {
				throw new PnInternalException( "Unable to deserialize lastEvaluatedKey", e );
			}
		} else {
			log.debug( "First page search" );
		}

		//devo opacizzare i campi di ricerca
		if (searchDto.getFilterId() != null && searchDto.isBySender() && !searchDto.isReceiverIdIsOpaque() ) {
			log.info( "[start] Send request to data-vault" );
			String opaqueTaxId = dataVaultClient.ensureRecipientByExternalId( RecipientType.PF, searchDto.getFilterId() );
			log.info( "[end] Ensured recipient for search" );
			searchDto.setFilterId( opaqueTaxId );
		} else {
			log.debug( "No filterId or search is by receiver" );
		}

		NotificationSearch pageSearch = notificationSearchFactory.getMultiPageSearch(
				searchDto,
				lastEvaluatedKey);

		log.debug( "START search notification metadata" );
		ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchResult = pageSearch.searchNotificationMetadata();
		log.debug( "END search notification metadata" );

		ResultPaginationDto.ResultPaginationDtoBuilder<NotificationSearchRow,String> builder = ResultPaginationDto.builder();
		builder.moreResult(searchResult.isMoreResult() )
				.resultsPage( searchResult.getResultsPage() );
		if ( !CollectionUtils.isEmpty(searchResult.getNextPagesKey()) ) {
			builder.nextPagesKey( searchResult.getNextPagesKey()
					.stream().map(PnLastEvaluatedKey::serializeInternalLastEvaluatedKey)
					.collect(Collectors.toList()) );
		}
		else
			builder.nextPagesKey(new ArrayList<>());
		return builder.build();
	}

	/**
	 * Check mandates for uid and cx-id
	 *
	 * @param searchDto search input data
	 * @param mandateId mandate id
	 * @throws PnNotFoundException if no valid mandate for delegator, receiver
	 *
	 *
	 */
	private void checkMandate(InputSearchNotificationDto searchDto, String mandateId) {
		String senderReceiverId = searchDto.getSenderReceiverId();
		log.info( "START check mandate for receiverId={} and manadteId={}", senderReceiverId, mandateId );
		List<InternalMandateDto> mandates = this.pnMandateClient.listMandatesByDelegate(senderReceiverId, mandateId);
		if(!mandates.isEmpty()) {
			boolean validMandate = false;
			for ( InternalMandateDto mandate : mandates ) {
				if (mandate.getDelegator() != null && mandate.getDatefrom() != null && mandate.getMandateId() != null && mandate.getMandateId().equals(mandateId)) {
					adjustSearchDatesAndReceiver( searchDto, mandate );
					validMandate = true;
					log.info( "Valid mandate for delegate={}", senderReceiverId );
					break;
				}
			}
			if (!validMandate){
				String message = String.format("Unable to find valid mandate for delegate=%s with mandateId=%s", senderReceiverId, mandateId);
				log.error( message );
				throw new PnNotFoundException( message );
			}
		} else {
			String message = String.format("Unable to find any mandate for delegate=%s with mandateId=%s", senderReceiverId, mandateId);
			log.error( message );
			throw new PnNotFoundException( message );
		}
		log.info( "END check mandate for receiverId={} and manadteId={}", senderReceiverId, mandateId );
	}

	/**
	 * Adjust search range date and receiver with mandate info
	 *
	 * @param searchDto search input data
	 * @param mandate mandate object
	 *
	 *
	 */
	private void adjustSearchDatesAndReceiver(InputSearchNotificationDto searchDto,
											  InternalMandateDto mandate) {
		Instant searchStartDate = searchDto.getStartDate();
		Instant searchEndDate = searchDto.getEndDate();
		Instant mandateStartDate = Instant.parse(mandate.getDatefrom());
		Instant mandateEndDate = mandate.getDateto() != null ? Instant.parse(mandate.getDateto()) : null;
		searchDto.setStartDate( searchStartDate.isBefore(mandateStartDate)? mandateStartDate : searchStartDate );
		if (mandateEndDate != null) {
			searchDto.setEndDate( searchEndDate.isBefore(mandateEndDate) ? searchEndDate : mandateEndDate );
		}
		log.debug( "Adjust search date, startDate={} endDate={}", searchDto.getStartDate(), searchDto.getEndDate() );

		String delegator = mandate.getDelegator();
		if (StringUtils.hasText( delegator )) {
			searchDto.setSenderReceiverId( delegator );
		}
		log.debug( "Adjust receiverId={}", delegator );
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
	 * @param withTimeline true if return Notification with Timeline and StatusHistory
	 * @param requestBySender true if the request came from Sender
	 *
	 * @return Notification DTO
	 *
	 */
	public InternalNotification getNotificationInformation(String iun, boolean withTimeline, boolean requestBySender) {
		log.debug( "Retrieve notification by iun={} withTimeline={} requestBySender={} START", iun, withTimeline, requestBySender );
		Optional<InternalNotification> optNotification = notificationDao.getNotificationByIun(iun);

		if (optNotification.isPresent()) {
			InternalNotification notification = optNotification.get();
			if (withTimeline) {
				notification = enrichWithTimelineAndStatusHistory(iun, notification);
				Date refinementDate = findRefinementDate( notification.getTimeline(), notification.getIun() );
				checkDocumentsAvailability( notification, refinementDate );
				if ( !requestBySender ) {
					computeNoticeCodeToReturn( notification, refinementDate );
				}
			}
			return notification;
		} else {
			String msg = String.format( "Error retrieving Notification with iun=%s withTimeline=%b", iun, withTimeline );
			log.debug( msg );
			throw new PnInternalException( msg );
		}
	}

	private Date findRefinementDate(List<TimelineElement> timeline, String iun) {
		log.debug( "Find refinement date iun={}", iun );
		Date refinementDate = null;
		// cerco elemento timeline con category refinement o notificationView
		List<TimelineElement> timelineElementList = timeline
				.stream()
				.filter(tle -> TimelineElementCategory.REFINEMENT.equals( tle.getCategory() ) || TimelineElementCategory.NOTIFICATION_VIEWED.equals( tle.getCategory() ))
				.collect(Collectors.toList());
		// se trovo la data di perfezionamento della notifica
		if (!timelineElementList.isEmpty()) {
			Optional<TimelineElement> optionalMin = timelineElementList.stream().min(Comparator.comparing(TimelineElement::getTimestamp));
			if (optionalMin.isPresent()) {
				refinementDate = optionalMin.get().getTimestamp();
			}
		} else {
			log.debug( "Notification iun={} not perfected", iun );
		}
		return refinementDate;
	}

	private void computeNoticeCodeToReturn(InternalNotification notification, Date refinementDate) {
		log.debug( "Compute notice code to return for iun={}", notification.getIun() );
		NoticeCodeToReturn noticeCodeToReturn = findNoticeCodeToReturn(notification.getIun(), refinementDate);
		setNoticeCodeToReturn(notification.getRecipients(), noticeCodeToReturn, notification.getIun());
	}

	private NoticeCodeToReturn findNoticeCodeToReturn(String iun, Date refinementDate) {
		// restituire il primo notice code se notifica ancora non perfezionata o perfezionata da meno di 5 gg
		NoticeCodeToReturn noticeCodeToReturn = NoticeCodeToReturn.FIRST_NOTICE_CODE;
		if ( refinementDate != null ) {
			long daysBetween = ChronoUnit.DAYS.between( refinementDate.toInstant(), Instant.now() );
			// restituire il secondo notice code se data perfezionamento tra 5 e 60 gg da oggi
			if ( daysBetween > MAX_FIRST_NOTICE_CODE_DAYS && daysBetween <= MAX_SECOND_NOTICE_CODE_DAYS) {
				log.debug( "Return second notice code for iun={}, days from refinement={}", iun, daysBetween );
				noticeCodeToReturn = NoticeCodeToReturn.SECOND_NOTICE_CODE;
			}
			// non restituire nessuno notice code se data perfezionamento più di 60 gg da oggi
			if ( daysBetween > MAX_SECOND_NOTICE_CODE_DAYS) {
				log.debug( "Return no notice code for iun={}, days from refinement={}", iun, daysBetween );
				noticeCodeToReturn = NoticeCodeToReturn.NO_NOTICE_CODE;
			}
		}
		return noticeCodeToReturn;
	}

	private void setNoticeCodeToReturn(List<NotificationRecipient> recipientList, NoticeCodeToReturn noticeCodeToReturn, String iun) {
		for ( NotificationRecipient recipient : recipientList ) {
			NotificationPaymentInfo paymentInfo = recipient.getPayment();
			if ( paymentInfo != null && paymentInfo.getNoticeCodeAlternative() != null ) {
				switch (noticeCodeToReturn) {
					case FIRST_NOTICE_CODE: {
						break;
					}
					case SECOND_NOTICE_CODE: {
						paymentInfo.setNoticeCode( paymentInfo.getNoticeCodeAlternative() );
						break;
					}
					case NO_NOTICE_CODE: {
						paymentInfo.setNoticeCode( null );
						break;
					}
					default: {
						throw new UnsupportedOperationException( "Unable to compute notice code to return for iun="+ iun );
					}
				}
				// in ogni caso non restituisco il noticeCode opzionale
				paymentInfo.setNoticeCodeAlternative( null );
			}
		}
	}

	public enum NoticeCodeToReturn {
		FIRST_NOTICE_CODE("FIRST_NOTICE_CODE"),
		SECOND_NOTICE_CODE("SECOND_NOTICE_CODE"),
		NO_NOTICE_CODE("NO_NOTICE_CODE");

		private String value;

		NoticeCodeToReturn(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

	public InternalNotification getNotificationInformation(String iun) {
		return getNotificationInformation( iun, true, false );
	}

	/**
	 * Get the full detail of a notification by IUN and notify viewed event
	 *
	 * @param iun    unique identifier of a Notification
	 * @param userId identifier of a user
	 * @return Notification
	 */
	public InternalNotification getNotificationAndNotifyViewedEvent(String iun, String userId, String mandateId) {
		log.debug("Start getNotificationAndSetViewed for {}", iun);

		String delegatorId = null;
		if (mandateId != null) {
			delegatorId = checkMandateForNotificationDetail(userId, mandateId);
		}

		InternalNotification notification = getNotificationInformation(iun);
		handleNotificationViewedEvent(iun, delegatorId != null? delegatorId : userId, notification);
		return notification;
	}

	private String checkMandateForNotificationDetail(String userId, String mandateId) {
		String delegatorId = null;

		List<InternalMandateDto> mandates = this.pnMandateClient.listMandatesByDelegate(userId, mandateId);
		if(!mandates.isEmpty()) {
			boolean validMandate = false;
			for ( InternalMandateDto mandate : mandates ) {
				if (mandate.getDelegator() != null && mandate.getMandateId() != null && mandate.getMandateId().equals(mandateId)) {
					delegatorId = mandate.getDelegator();
					validMandate = true;
					log.info( "Valid mandate for notification detail for delegate={}", userId );
					break;
				}
			}
			if (!validMandate){
				String message = String.format("Unable to find valid mandate for notification detail for delegate=%s with mandateId=%s", userId, mandateId);
				log.error( message );
				throw new PnNotFoundException( message );
			}
		} else {
			String message = String.format("Unable to find any mandate for notification detail for delegate=%s with mandateId=%s", userId, mandateId);
			log.error( message );
			throw new PnNotFoundException( message );
		}
		return delegatorId;
	}

	private void checkDocumentsAvailability(InternalNotification notification, Date refinementDate) {
		log.debug( "Check if documents are available for iun={}", notification.getIun() );
		notification.setDocumentsAvailable( true );
		if ( !NotificationStatus.CANCELLED.equals( notification.getNotificationStatus() ) ) {
			if ( refinementDate != null ) {
				long daysBetween = ChronoUnit.DAYS.between( refinementDate.toInstant(), Instant.now() );
				if ( daysBetween > MAX_DOCUMENTS_AVAILABLE_DAYS ) {
					log.debug("Documents not more available for iun={} from={}", notification.getIun(), refinementDate);
					removeDocuments( notification );
				}
			}
		} else {
			log.debug("Documents not more available for iun={} because is cancelled", notification.getIun());
			removeDocuments(notification);
		}
	}

	private void removeDocuments(InternalNotification notification) {
		notification.setDocumentsAvailable( false );
		notification.setDocuments( null );
		for ( NotificationRecipient recipient : notification.getRecipients() ) {
			NotificationPaymentInfo payment = recipient.getPayment();
			if ( payment != null ) {
				payment.setPagoPaForm( null );
				payment.setF24flatRate( null );
				payment.setF24standard( null );
			}
		}
	}

	private void handleNotificationViewedEvent(String iun, String userId, InternalNotification notification) {
		if (StringUtils.hasText(userId)) {
			notifyNotificationViewedEvent(notification, userId);
		} else {
			log.error("UserId is not present, can't create notification view event for iun={}", iun);
			throw new PnInternalException("UserId is not present, can't create notification view event for iun=" + iun);
		}
	}

	public InternalNotification enrichWithTimelineAndStatusHistory(String iun, InternalNotification notification) {
		log.debug( "Retrieve timeline for iun={}", iun );
		int numberOfRecipients = notification.getRecipients().size();
		Date createdAt =  notification.getSentAt();
		OffsetDateTime offsetDateTime = createdAt.toInstant()
				.atOffset(ZoneOffset.UTC);

		NotificationHistoryResponse timelineStatusHistoryDto =  pnDeliveryPushClient.getTimelineAndStatusHistory(iun,numberOfRecipients, offsetDateTime);


		List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> timelineList = timelineStatusHistoryDto.getTimeline()
				.stream()
				.sorted( Comparator.comparing(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement::getTimestamp))
				.collect(Collectors.toList());

		log.debug( "Retrieve status history for notification created at={}", createdAt );

		List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement> statusHistory = timelineStatusHistoryDto.getNotificationStatusHistory();

		ModelMapper mapperStatusHistory = new ModelMapper();
		mapperStatusHistory.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, NotificationStatusHistoryElement.class )
				.addMapping( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement::getActiveFrom, NotificationStatusHistoryElement::setActiveFrom );
		mapperStatusHistory.getConfiguration().setMatchingStrategy( MatchingStrategies.STRICT );
		Converter<OffsetDateTime,Date> dateConverter = ctx -> ctx.getSource() != null ? fromOffsetToDate( ctx.getSource() ) : null;
		mapperStatusHistory.addConverter( dateConverter, OffsetDateTime.class, Date.class );

		ModelMapper mapperNotification = modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class );

		ModelMapper mapperTimeline = new ModelMapper();
		mapperTimeline.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class )
				.addMapping(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement::getTimestamp, TimelineElement::setTimestamp );
		mapperTimeline.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
		mapperTimeline.addConverter( dateConverter, OffsetDateTime.class, Date.class );

		FullSentNotification resultFullSent = notification
				.timeline( timelineList.stream()
						.map( timelineElement -> mapperTimeline.map(timelineElement, TimelineElement.class ) )
						.collect(Collectors.toList())  )
				.notificationStatusHistory( statusHistory.stream()
						.map( el -> mapperStatusHistory.map( el, NotificationStatusHistoryElement.class ))
						.collect(Collectors.toList())
				)
				.notificationStatus( NotificationStatus.fromValue( timelineStatusHistoryDto.getNotificationStatus().getValue() ));

		return mapperNotification.map( resultFullSent, InternalNotification.class );
	}

	private Date fromOffsetToDate(OffsetDateTime source) {
		return Date.from( source.toInstant() );
	}


	private void notifyNotificationViewedEvent(InternalNotification notification, String userId) {
		String iun = notification.getIun();

		int recipientIndex = -1;
		for( int idx = 0 ; idx < notification.getRecipientIds().size(); idx++) {
			String recipientId = notification.getRecipientIds().get( idx );
			if( userId.equals( recipientId ) ) {
				recipientIndex = idx;
			}
		}

		if( recipientIndex == -1 ) {
			log.debug("Recipient not found for iun={} and userId={} ", iun, userId );
			throw new PnInternalException( "Notification with iun=" + iun + " do not have recipient/delegator=" + userId );
		}

		log.info("Send \"notification acknowlwdgement\" event for iun={}", iun);
		Instant createdAt = clock.instant();
		notificationAcknowledgementProducer.sendNotificationViewed( iun, createdAt, recipientIndex );
	}
}
