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
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
	public static final long MAX_FIRST_IUV_DAYS = 5L;
	public static final long MAX_SECOND_IUV_DAYS = 60L;

	private final Clock clock;
	private final NotificationViewedProducer notificationAcknowledgementProducer;
	private final NotificationDao notificationDao;
	private final PnDeliveryPushClientImpl pnDeliveryPushClient;
	private final PnDeliveryConfigs cfg;
	private final PnMandateClientImpl pnMandateClient;
	private final PnDataVaultClientImpl dataVaultClient;
	private final ModelMapperFactory modelMapperFactory;


	@Autowired
	public NotificationRetrieverService(Clock clock,
										NotificationViewedProducer notificationAcknowledgementProducer,
										NotificationDao notificationDao,
										PnDeliveryPushClientImpl pnDeliveryPushClient,
										PnDeliveryConfigs cfg,
										PnMandateClientImpl pnMandateClient, PnDataVaultClientImpl dataVaultClient, ModelMapperFactory modelMapperFactory) {
		this.clock = clock;
		this.notificationAcknowledgementProducer = notificationAcknowledgementProducer;
		this.notificationDao = notificationDao;
		this.pnDeliveryPushClient = pnDeliveryPushClient;
		this.cfg = cfg;
		this.pnMandateClient = pnMandateClient;
		this.dataVaultClient = dataVaultClient;
		this.modelMapperFactory = modelMapperFactory;
	}

	public ResultPaginationDto<NotificationSearchRow,String> searchNotification(InputSearchNotificationDto searchDto ) {
		log.info("Start search notification - senderReceiverId={}", searchDto.getSenderReceiverId());

		validateInput(searchDto);

		if ( !searchDto.isBySender() ) {
			String mandateId = searchDto.getMandateId();
			if (mandateId != null) {
				checkMandate(searchDto, mandateId);
			}
		}

		PnLastEvaluatedKey lastEvaluatedKey = null;
		if ( searchDto.getNextPagesKey() != null ) {
			try {
				lastEvaluatedKey = PnLastEvaluatedKey.deserializeInternalLastEvaluatedKey( searchDto.getNextPagesKey() );
			} catch (JsonProcessingException e) {
				throw new PnInternalException( "Unable to deserialize lastEvaluatedKey", e );
			}
		}

		//devo opacizzare i campi di ricerca
		if (searchDto.getFilterId() != null && searchDto.isBySender() ) {
			log.info( "[start] Send request to data-vault" );
			String opaqueTaxId = dataVaultClient.ensureRecipientByExternalId( RecipientType.PF, searchDto.getFilterId() );
			log.info( "[end] Ensured recipient for search" );
			searchDto.setFilterId( opaqueTaxId );
		}

		MultiPageSearch multiPageSearch = new MultiPageSearch(
				notificationDao,
				searchDto,
				lastEvaluatedKey,
				cfg,
				dataVaultClient);

		ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchResult = multiPageSearch.searchNotificationMetadata();

		ResultPaginationDto.ResultPaginationDtoBuilder<NotificationSearchRow,String> builder = ResultPaginationDto.builder();
		builder.moreResult( searchResult.getNextPagesKey() != null )
				.resultsPage( searchResult.getResultsPage() );
		if ( searchResult.getNextPagesKey() != null ) {
			builder.nextPagesKey( searchResult.getNextPagesKey()
					.stream().map(PnLastEvaluatedKey::serializeInternalLastEvaluatedKey)
					.collect(Collectors.toList()) );
		}
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
		if (StringUtils.isNotBlank( delegator )) {
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
	 *
	 * @return Notification DTO
	 *
	 */
	public InternalNotification getNotificationInformation(String iun, boolean withTimeline) {
		log.debug( "Retrieve notification by iun={} withTimeline={} START", iun, withTimeline );
		Optional<InternalNotification> optNotification = notificationDao.getNotificationByIun(iun);

		if (optNotification.isPresent()) {
			InternalNotification notification = optNotification.get();
			if (withTimeline) {
				notification = enrichWithTimelineAndStatusHistory(iun, notification);
				setIsDocumentsAvailable( notification );
				computeIuvToReturn( notification );
			}
			return notification;
		} else {
			String msg = String.format( "Error retrieving Notification with iun=%s withTimeline=%b", iun, withTimeline );
			log.debug( msg );
			throw new PnInternalException( msg );
		}
	}

	private void computeIuvToReturn(InternalNotification notification) {
		log.debug( "Set IUV to return" );
		// cerco elemento timeline con category refinement o notificationView
		List<TimelineElement> timelineElementList = notification.getTimeline()
				.stream()
				.filter(tle -> TimelineElementCategory.REFINEMENT.equals( tle.getCategory() ) || TimelineElementCategory.NOTIFICATION_VIEWED.equals( tle.getCategory() ))
				.collect(Collectors.toList());
		// se trovo la data di perfezionamento della notifica
		if (!timelineElementList.isEmpty()) {
			findAndSetIUVToReturn(notification, timelineElementList);
		}
	}

	private void findAndSetIUVToReturn(InternalNotification notification, List<TimelineElement> timelineElementList) {
		Integer iuvToReturn = null;
		Optional<TimelineElement> optionalMin = timelineElementList.stream().min( Comparator.comparing( TimelineElement::getTimestamp ) );
		if (optionalMin.isPresent()) {
			Date refinementDate = optionalMin.get().getTimestamp();
			long daysBetween = ChronoUnit.DAYS.between( refinementDate.toInstant(), Instant.now() );
			if (daysBetween <= MAX_FIRST_IUV_DAYS) {
				log.debug( "Return first IUV for iun={}, days from refinement={}", notification.getIun(), daysBetween );
				iuvToReturn = 1;
			}
			if ( daysBetween > MAX_FIRST_IUV_DAYS && daysBetween <= MAX_SECOND_IUV_DAYS ) {
				log.debug( "Return second IUV for iun={}, days from refinement={}", notification.getIun(), daysBetween );
				iuvToReturn = 2;
			}
			if ( daysBetween > MAX_SECOND_IUV_DAYS ) {
				log.debug( "Return no IUV for iun={}, days from refinement={}", notification.getIun(), daysBetween );
				iuvToReturn = 0;
			}
			setIuvToReturn(notification, iuvToReturn);
		}
	}

	private void setIuvToReturn(InternalNotification notification, Integer iuvToReturn) {
		List<NotificationRecipient> recipientList = notification.getRecipients();
		for ( NotificationRecipient recipient : recipientList ) {
			NotificationPaymentInfo paymentInfo = recipient.getPayment();
			if ( paymentInfo != null ) {
				if (paymentInfo.getNoticeCodeOptional() != null && paymentInfo.getCreditorTaxIdOptional() != null) {
					switch (iuvToReturn) {
						case 1: {
							paymentInfo.setCreditorTaxIdOptional( null );
							paymentInfo.setNoticeCodeOptional( null );
							break;
						}
						case 2: {
							paymentInfo.setCreditorTaxId( paymentInfo.getCreditorTaxIdOptional() );
							paymentInfo.setNoticeCode( paymentInfo.getNoticeCodeOptional() );
							paymentInfo.setCreditorTaxIdOptional( null );
							paymentInfo.setNoticeCodeOptional( null );
							break;
						}
						case 0: {
							paymentInfo.setCreditorTaxId( null );
							paymentInfo.setNoticeCode( null );
							paymentInfo.setCreditorTaxIdOptional( null );
							paymentInfo.setNoticeCodeOptional( null );
							break;
						}
						default: {
							paymentInfo.setCreditorTaxIdOptional( null );
							paymentInfo.setNoticeCodeOptional( null );
						}
					}
				}
			}
		}
	}

	public InternalNotification getNotificationInformation(String iun) {
		return getNotificationInformation( iun, true );
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

	private void setIsDocumentsAvailable(InternalNotification notification) {
		log.debug( "Documents available for iun={}", notification.getIun() );
		notification.setDocumentsAvailable( true );
		// cerco elemento timeline con category refinement o notificationView
		List<TimelineElement> timelineElementList = notification.getTimeline()
				.stream()
				.filter(tle -> TimelineElementCategory.REFINEMENT.equals( tle.getCategory() ) || TimelineElementCategory.NOTIFICATION_VIEWED.equals( tle.getCategory() ))
				.collect(Collectors.toList());
		// se trovo elemento confronto con data odierna e se differenza > 120 gg allora documentsAvailable = false
		if (!timelineElementList.isEmpty()) {
			Optional<TimelineElement> optionalMin = timelineElementList.stream().min( Comparator.comparing( TimelineElement::getTimestamp ) );
			if ( optionalMin.isPresent() ) {
				Date refinementDate = optionalMin.get().getTimestamp();
				long daysBetween = ChronoUnit.DAYS.between( refinementDate.toInstant(), Instant.now() );
				if ( daysBetween > MAX_DOCUMENTS_AVAILABLE_DAYS) {
					log.debug( "Documents not more available for iun={} from={}", notification.getIun(), refinementDate );
					notification.setDocumentsAvailable( false );
				}
			}
		}
	}

	private void handleNotificationViewedEvent(String iun, String userId, InternalNotification notification) {
		if (StringUtils.isNotBlank(userId)) {
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
