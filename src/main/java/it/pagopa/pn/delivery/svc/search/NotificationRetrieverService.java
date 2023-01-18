package it.pagopa.pn.delivery.svc.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.*;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.*;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.externalregistries.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.clients.externalregistries.model.PaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.externalregistries.model.PaymentStatus;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.*;
import static it.pagopa.pn.delivery.utils.PgUtils.checkAuthorizationPGAndValuedGroups;

@Service
@Slf4j
public class NotificationRetrieverService {

	private static final Instant PN_EPOCH = Instant.ofEpochSecond( 1651399200 ); // 2022-05-01T12:00:00.000 GMT+2:00

	private final Clock clock;
	private final NotificationViewedProducer notificationAcknowledgementProducer;
	private final NotificationDao notificationDao;
	private final PnDeliveryPushClientImpl pnDeliveryPushClient;
	private final PnMandateClientImpl pnMandateClient;
	private final PnDataVaultClientImpl dataVaultClient;
	private final PnExternalRegistriesClientImpl pnExternalRegistriesClient;
	private final ModelMapperFactory modelMapperFactory;
	private final NotificationSearchFactory notificationSearchFactory;
	private final RefinementLocalDate refinementLocalDateUtils;
	private final MVPParameterConsumer mvpParameterConsumer;
	private final PnDeliveryConfigs cfg;


	@Autowired
	public NotificationRetrieverService(Clock clock,
										NotificationViewedProducer notificationAcknowledgementProducer,
										NotificationDao notificationDao,
										PnDeliveryPushClientImpl pnDeliveryPushClient,
										PnMandateClientImpl pnMandateClient,
										PnDataVaultClientImpl dataVaultClient,
										PnExternalRegistriesClientImpl pnExternalRegistriesClient,
										ModelMapperFactory modelMapperFactory,
										NotificationSearchFactory notificationSearchFactory,
										RefinementLocalDate refinementLocalDateUtils,
										MVPParameterConsumer mvpParameterConsumer,
										PnDeliveryConfigs cfg) {
		this.clock = clock;
		this.notificationAcknowledgementProducer = notificationAcknowledgementProducer;
		this.notificationDao = notificationDao;
		this.pnDeliveryPushClient = pnDeliveryPushClient;
		this.pnMandateClient = pnMandateClient;
		this.dataVaultClient = dataVaultClient;
		this.pnExternalRegistriesClient = pnExternalRegistriesClient;
		this.modelMapperFactory = modelMapperFactory;
		this.notificationSearchFactory = notificationSearchFactory;
		this.refinementLocalDateUtils = refinementLocalDateUtils;
		this.mvpParameterConsumer = mvpParameterConsumer;
		this.cfg = cfg;
	}

	public ResultPaginationDto<NotificationSearchRow, String> searchNotification(InputSearchNotificationDto searchDto,
																				 @Nullable String recipientType,
																				 @Nullable List<String> cxGroups) {

		Instant startDate = searchDto.getStartDate();
		if( PN_EPOCH.isAfter(startDate) ) {
			log.info("Start date is={} but Piattaforma Notifiche exists since={} ", startDate, PN_EPOCH);
			searchDto.setStartDate( PN_EPOCH );
		}
		// controllo endDate di ricerca sia dopo 2022-05-01T12:00:00.000 GMT+2:00
		Instant endDate = searchDto.getEndDate();
		if( PN_EPOCH.isAfter( endDate ) ) {
			log.info("End date is={} but Piattaforma Notifiche exists since={}", endDate, PN_EPOCH);
			return ResultPaginationDto.<NotificationSearchRow, String>builder()
					.resultsPage( Collections.emptyList() )
					.nextPagesKey( Collections.emptyList() )
					.moreResult( false )
					.build();
		}

		log.info("Start search notification - senderReceiverId={}", searchDto.getSenderReceiverId());

		validateInput(searchDto);

		if ( !searchDto.isBySender() ) {
			log.debug( "Search from receiver" );
			String mandateId = searchDto.getMandateId();
			if ( StringUtils.hasText( mandateId )) {
				checkMandate(searchDto, mandateId, recipientType, cxGroups);
			}
			else if(checkAuthorizationPGAndValuedGroups(recipientType, cxGroups)) {
				log.error( "only a PG admin can access this resource" );
				throw new PnForbiddenException();
			}
		}

		PnLastEvaluatedKey lastEvaluatedKey = null;
		if ( searchDto.getNextPagesKey() != null ) {
			try {
				lastEvaluatedKey = PnLastEvaluatedKey.deserializeInternalLastEvaluatedKey( searchDto.getNextPagesKey() );
			} catch (JsonProcessingException e) {
				throw new PnInternalException( "Unable to deserialize lastEvaluatedKey",
						ERROR_CODE_DELIVERY_UNSUPPORTED_LAST_EVALUATED_KEY,
						e );
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

		// labelize groups
		labelizeGroups(searchResult, searchDto.getSenderReceiverId());

		ResultPaginationDto.ResultPaginationDtoBuilder<NotificationSearchRow,String> builder = ResultPaginationDto.builder();
		builder.moreResult(searchResult.isMoreResult() )
				.resultsPage( searchResult.getResultsPage() );
		if ( !CollectionUtils.isEmpty(searchResult.getNextPagesKey()) ) {
			builder.nextPagesKey( searchResult.getNextPagesKey()
					.stream().map(PnLastEvaluatedKey::serializeInternalLastEvaluatedKey)
					.toList() );
		}
		else
			builder.nextPagesKey(new ArrayList<>());
		return builder.build();
	}

	public ResultPaginationDto<NotificationSearchRow, String> searchNotificationDelegated(InputSearchNotificationDelegatedDto searchDto) {
		Instant startDate = searchDto.getStartDate();
		if (PN_EPOCH.isAfter(startDate)) {
			log.info("start date {} but PN exists since {}", startDate, PN_EPOCH);
			searchDto.setStartDate(PN_EPOCH);
		}
		Instant endDate = searchDto.getEndDate();
		if (PN_EPOCH.isAfter(endDate)) {
			log.info("end date {} but PN exists since {}", endDate, PN_EPOCH);
			return ResultPaginationDto.<NotificationSearchRow, String>builder()
					.resultsPage(Collections.emptyList())
					.nextPagesKey(Collections.emptyList())
					.moreResult(false)
					.build();
		}

		if (!CollectionUtils.isEmpty(searchDto.getCxGroups())
				&& (!StringUtils.hasText(searchDto.getGroup()) || !searchDto.getCxGroups().contains(searchDto.getGroup()))) {
			log.warn("user with cx-groups {} can not access notification delegated to group {}", searchDto.getCxGroups(), searchDto.getGroup());
			throw new PnForbiddenException();
		}

		log.info("start search delegated notification - delegateId={}", searchDto.getDelegateId());

		validateInput(searchDto);

		PnLastEvaluatedKey lastEvaluatedKey = null;

		if (searchDto.getNextPageKey() != null) {
			try {
				lastEvaluatedKey = PnLastEvaluatedKey.deserializeInternalLastEvaluatedKey(searchDto.getNextPageKey());
			} catch (JsonProcessingException e) {
				throw new PnInternalException("Unable to deserialize lastEvaluatedKey", ERROR_CODE_DELIVERY_UNSUPPORTED_LAST_EVALUATED_KEY, e);
			}
		} else {
			log.debug("first page search");
		}

		NotificationSearch page = notificationSearchFactory.getMultiPageDelegatedSearch(searchDto, lastEvaluatedKey);
		log.debug("START search notification delegation metadata");
		ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = page.searchNotificationMetadata();
		log.debug("END search notification delegation metadata");

		ResultPaginationDto.ResultPaginationDtoBuilder<NotificationSearchRow, String> builder = ResultPaginationDto.builder();
		builder.moreResult(result.isMoreResult())
				.resultsPage(result.getResultsPage());
		if (!CollectionUtils.isEmpty(result.getNextPagesKey())) {
			builder.nextPagesKey(result.getNextPagesKey().stream()
							.map(PnLastEvaluatedKey::serializeInternalLastEvaluatedKey)
							.toList())
					.build();
		} else {
			builder.nextPagesKey(new ArrayList<>());
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
	private void checkMandate(InputSearchNotificationDto searchDto, String mandateId, String recipientType, List<String> cxGroups) {
		String senderReceiverId = searchDto.getSenderReceiverId();
		log.info( "START check mandate for receiverId={} and manadteId={}", senderReceiverId, mandateId );
		List<InternalMandateDto> mandates = pnMandateClient.listMandatesByDelegate(senderReceiverId, mandateId, CxTypeAuthFleet.valueOf(recipientType), cxGroups);
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
				throw new PnMandateNotFoundException( message );
			}
		} else {
			String message = String.format("Unable to find any mandate for delegate=%s with mandateId=%s", senderReceiverId, mandateId);
			log.error( message );
			throw new PnMandateNotFoundException(  message );
		}
		log.info( "END check mandate for receiverId={} and mandateId={}", senderReceiverId, mandateId );
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
			List<ProblemError> errorList  = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
			throw new PnInvalidInputException(searchDto.getSenderReceiverId(), errorList);
		}

		log.debug("Validation search input OK - senderReceiverId {}",searchDto.getSenderReceiverId());
	}

	private void validateInput(InputSearchNotificationDelegatedDto searchDto) {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			Validator validator = factory.getValidator();
			Set<ConstraintViolation<InputSearchNotificationDelegatedDto>> errors = validator.validate(searchDto);
			if (!errors.isEmpty()) {
				log.error("validation search input failed - delegateId {} - errors: {}", searchDto.getDelegateId(), errors);
				List<ProblemError> errorList = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
				throw new PnInvalidInputException(searchDto.getDelegateId(), errorList);
			}
		}
		log.debug("validation search input succeeded - delegateId {}", searchDto.getDelegateId());
	}

	private InternalNotification getInternalNotification(String iun) {
		Optional<InternalNotification> optNotification = notificationDao.getNotificationByIun(iun);
		if (optNotification.isPresent()) {
			return optNotification.get();
		} else {
			String msg = String.format( "Error retrieving Internal Notification with iun=%s", iun );
			log.debug( msg );
			throw new PnNotificationNotFoundException(  msg );
		}
	}

	/**
	 * Get the full detail of a notification by IUN
	 *
	 * @param iun unique identifier of a Notification
	 * @param withTimeline true if return Notification with Timeline and StatusHistory
	 * @param requestBySender true if the request came from Sender
	 * @param senderId unique identifier of the sender
	 *
	 * @return Notification DTO
	 *
	 */
	public InternalNotification getNotificationInformation(String iun, boolean withTimeline, boolean requestBySender, String senderId) {
		log.debug( "Retrieve notification by iun={} withTimeline={} requestBySender={} START", iun, withTimeline, requestBySender );
		InternalNotification notification = getInternalNotification( iun );
		if (withTimeline) {
			completeInternalNotificationWithTimeline(iun, requestBySender, notification);
		}
		labelizeGroup(notification, senderId);
		return notification;
	}

	private void completeInternalNotificationWithTimeline(String iun, boolean requestBySender, InternalNotification notification) {
		notification = enrichWithTimelineAndStatusHistory(iun, notification);
		OffsetDateTime refinementDate = findRefinementDate( notification.getTimeline(), notification.getIun() );
		checkDocumentsAvailability(notification, refinementDate );
		if ( !requestBySender && Boolean.TRUE.equals( mvpParameterConsumer.isMvp( notification.getSenderTaxId() ) ) ) {
			computeNoticeCodeToReturn(notification, refinementDate );
		}
	}

	/**
	 * Get the full detail of a notification by IUN with senderId check
	 *
	 * @param iun unique identifier of a Notification
	 * @param senderId unique identifier of the sender
	 * @throws PnNotificationNotFoundException if sender is not notification sender
	 *
	 * @return Notification DTO
	 *
	 */
	public InternalNotification getNotificationInformationWithSenderIdCheck(String iun, String senderId) {
		log.debug( "Retrieve complete notification with sender check by iun={} senderId={} START", iun, senderId );
		InternalNotification notification = getInternalNotification( iun );
		checkSenderId( iun, notification.getSenderPaId(), senderId );
		completeInternalNotificationWithTimeline(iun, true, notification);
		labelizeGroup(notification, senderId);
		return notification;
	}

	private void checkSenderId(String iun, String notificationSenderPaId, String senderId) {
		if ( !notificationSenderPaId.equals( senderId ) )
			throw new PnNotificationNotFoundException(
					String.format("Unable to find notification with iun=%s for senderId=%s", iun, senderId  )
			);
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
		return getNotificationInformation(iun, withTimeline, requestBySender, null);
	}

	protected OffsetDateTime findRefinementDate(List<TimelineElement> timeline, String iun) {
		log.debug( "Find refinement date iun={}", iun );
		OffsetDateTime refinementDate = null;
		// cerco elemento timeline con category refinement o notificationView
		Optional<TimelineElement> optionalMin = timeline
				.stream()
				.filter(tle -> TimelineElementCategory.REFINEMENT.equals(tle.getCategory() )
								|| TimelineElementCategory.NOTIFICATION_VIEWED.equals( tle.getCategory() ))
				.min( Comparator.comparing(TimelineElement::getTimestamp) );
		// se trovo la data di perfezionamento della notifica
		if (optionalMin.isPresent()) {
			refinementDate = refinementLocalDateUtils.setLocalRefinementDate(optionalMin.get());
		} else {
			log.debug( "Notification iun={} not perfected", iun );
		}
		return refinementDate;
	}

	private void computeNoticeCodeToReturn(InternalNotification notification, OffsetDateTime refinementDate) {
		log.debug( "Compute notice code to return for iun={}", notification.getIun() );
		NoticeCodeToReturn noticeCodeToReturn = findNoticeCodeToReturn(notification.getIun(), refinementDate);
		setNoticeCodeToReturn(notification.getRecipients(), noticeCodeToReturn, notification.getIun());
	}

	private NoticeCodeToReturn findNoticeCodeToReturn(String iun, OffsetDateTime refinementDate) {
		// restituire il primo notice code se notifica ancora non perfezionata o perfezionata da meno di 5 gg
		NoticeCodeToReturn noticeCodeToReturn = NoticeCodeToReturn.FIRST_NOTICE_CODE;
		if ( refinementDate != null ) {
			long daysBetween = ChronoUnit.DAYS.between( refinementDate.toInstant().truncatedTo(ChronoUnit.DAYS),
					clock.instant().truncatedTo( ChronoUnit.DAYS ) );
			// restituire il secondo notice code se data perfezionamento tra 5 e 60 gg da oggi
			long maxFirstNoticeCodeDays = Long.parseLong( cfg.getMaxFirstNoticeCodeDays() );
			long maxSecondNoticeCodeDays = Long.parseLong( cfg.getMaxSecondNoticeCodeDays() );
			if ( daysBetween > maxFirstNoticeCodeDays && daysBetween <= maxSecondNoticeCodeDays) {
				log.debug( "Return second notice code for iun={}, days from refinement={}", iun, daysBetween );
				noticeCodeToReturn = NoticeCodeToReturn.SECOND_NOTICE_CODE;
			}
			// non restituire nessuno notice code se data perfezionamento più di 60 gg da oggi
			if ( daysBetween > maxSecondNoticeCodeDays) {
				log.debug( "Return no notice code for iun={}, days from refinement={}", iun, daysBetween );
				noticeCodeToReturn = NoticeCodeToReturn.NO_NOTICE_CODE;
			}
		}
		return noticeCodeToReturn;
	}

	private void setNoticeCodeToReturn(List<NotificationRecipient> recipientList, NoticeCodeToReturn noticeCodeToReturn, String iun) {
		// - per l'MVP la notifica ha necessariamente un solo destinatario
		for ( NotificationRecipient recipient : recipientList ) {
			NotificationPaymentInfo notificationPaymentInfo = recipient.getPayment();
			if ( notificationPaymentInfo != null) {
    			String creditorTaxId = notificationPaymentInfo.getCreditorTaxId();
    			String noticeCode = notificationPaymentInfo.getNoticeCode();
    			if ( notificationPaymentInfo.getNoticeCodeAlternative() != null ) {
    				switch (noticeCodeToReturn) {
    					case FIRST_NOTICE_CODE: {
    						break;
    					}
    					// - se devo restituire il notice code alternativo...
    					case SECOND_NOTICE_CODE: {
    						// - ...verifico che il primo notice code non è stato già pagato
    						setNoticeCodePayment(iun, notificationPaymentInfo, creditorTaxId, noticeCode);
    						break;
    					}
    					case NO_NOTICE_CODE: {
    						notificationPaymentInfo.setNoticeCode( null );
    						break;
    					}
    					default: {
    						throw new UnsupportedOperationException( "Unable to compute notice code to return for iun="+ iun );
    					}
    				}
    				// in ogni caso non restituisco il noticeCode opzionale
    				notificationPaymentInfo.setNoticeCodeAlternative( null );
    			}
			}
		}
	}

	private void setNoticeCodePayment(String iun, NotificationPaymentInfo notificationPaymentInfo, String creditorTaxId, String noticeCode) {
		log.debug( "Start getPaymentInfo iun={} creditorTaxId={} noticeCode={}", iun, creditorTaxId, noticeCode);
		try {
			PaymentInfo paymentInfo = this.pnExternalRegistriesClient.getPaymentInfo(creditorTaxId, noticeCode);
			if ( paymentInfo != null ) {
				log.debug( "End getPaymentInfo iun={} creditorTaxId={} noticeCode={} paymentStatus={}", iun, creditorTaxId, noticeCode, paymentInfo.getStatus() );
				// - se il primo notice code NON è stato già pagato
				if ( !PaymentStatus.SUCCEEDED.equals( paymentInfo.getStatus() ) ) {
					// - restituisco il notice code alternativo
					log.info( "Return for iun={} alternative notice code={}", iun, notificationPaymentInfo.getNoticeCodeAlternative() );
					notificationPaymentInfo.setNoticeCode( notificationPaymentInfo.getNoticeCodeAlternative() );
				}
				// - il primo notice code è stato già pagato quindi lo restituisco
			} else {
				// - External-registries non risponde quindi non restituisco nessun notice code
				log.debug( "Unable to getPaymentInfo iun={} creditorTaxId={} noticeCode={}", iun, creditorTaxId, noticeCode);
				notificationPaymentInfo.setNoticeCode( null );
			}
		} catch ( PnHttpResponseException ex ) {
			// - External-registries non risponde quindi non restituisco nessun notice code
			log.error( "Unable to getPaymentInfo iun={} creditorTaxId={} noticeCode={} caused by ex={}", iun, creditorTaxId, noticeCode, ex);
			notificationPaymentInfo.setNoticeCode( null );
		}

	}

	public enum NoticeCodeToReturn {
		FIRST_NOTICE_CODE("FIRST_NOTICE_CODE"),
		SECOND_NOTICE_CODE("SECOND_NOTICE_CODE"),
		NO_NOTICE_CODE("NO_NOTICE_CODE");

		private final String value;

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

	public InternalNotification getNotificationInformation(String senderId, String paProtocolNumber, String idempotenceToken) {
		Optional<String> optionalRequestId = notificationDao.getRequestId( senderId, paProtocolNumber, idempotenceToken );
		if (optionalRequestId.isEmpty()) {
			String msg = String.format( "Unable to find requestId for senderId=%s paProtocolNumber=%s idempotenceToken=%s", senderId, paProtocolNumber, idempotenceToken );
			throw new PnNotificationNotFoundException(msg);
		}
		String iun = new String( Base64Utils.decodeFromString( optionalRequestId.get() ) );
		return getNotificationInformationWithSenderIdCheck( iun, senderId );
	}

	/**
	 * Get the full detail of a notification by IUN and notify viewed event
	 *
	 * @param iun    unique identifier of a Notification
	 * @param userId identifier of a user
	 * @param recipientType type of user (PF, PG)
	 * @param cxGroups user groups
	 * @return Notification
	 */
	public InternalNotification getNotificationAndNotifyViewedEvent(String iun, String userId, String mandateId, String recipientType, List<String> cxGroups) {
		log.debug("Start getNotificationAndSetViewed for {}", iun);

		String delegatorId = null;
		if (mandateId != null) {
			delegatorId = checkMandateForNotificationDetail(userId, mandateId, recipientType, cxGroups);
		} else if (checkAuthorizationPGAndValuedGroups(recipientType, cxGroups)) {
			log.error( "only a PG admin can access this resource" );
			throw new PnForbiddenException();
		}

		InternalNotification notification = getNotificationInformation(iun);
		notifyNotificationViewedEvent(notification, delegatorId != null? delegatorId : userId);
		return notification;
	}

	private String checkMandateForNotificationDetail(String userId, String mandateId, String recipientType, List<String> cxGroups) {
		String delegatorId = null;

		List<InternalMandateDto> mandates = pnMandateClient.listMandatesByDelegate(userId, mandateId, CxTypeAuthFleet.valueOf(recipientType), cxGroups);
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
				throw new PnMandateNotFoundException( message );
			}
		} else {
			String message = String.format("Unable to find any mandate for notification detail for delegate=%s with mandateId=%s", userId, mandateId);
			log.error( message );
			throw new PnMandateNotFoundException( message );
		}
		return delegatorId;
	}

	private void checkDocumentsAvailability(InternalNotification notification, OffsetDateTime refinementDate) {
		log.debug( "Check if documents are available for iun={}", notification.getIun() );
		notification.setDocumentsAvailable( true );
		if ( !NotificationStatus.CANCELLED.equals( notification.getNotificationStatus() ) ) {
			if ( refinementDate != null ) {
				long daysBetween = ChronoUnit.DAYS.between( refinementDate.toInstant().truncatedTo( ChronoUnit.DAYS ),
						clock.instant().truncatedTo( ChronoUnit.DAYS ) );
				if ( daysBetween > Long.parseLong( cfg.getMaxDocumentsAvailableDays() ) ) {
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
		notification.setDocuments( Collections.emptyList() );
		for ( NotificationRecipient recipient : notification.getRecipients() ) {
			NotificationPaymentInfo payment = recipient.getPayment();
			if ( payment != null ) {
				payment.setPagoPaForm( null );
				payment.setF24flatRate( null );
				payment.setF24standard( null );
			}
		}
	}

	public InternalNotification enrichWithTimelineAndStatusHistory(String iun, InternalNotification notification) {
		log.debug( "Retrieve timeline for iun={}", iun );
		int numberOfRecipients = notification.getRecipients().size();
		OffsetDateTime createdAt =  notification.getSentAt();

		NotificationHistoryResponse timelineStatusHistoryDto =  pnDeliveryPushClient.getTimelineAndStatusHistory(iun,numberOfRecipients, createdAt);


		List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> timelineList = timelineStatusHistoryDto.getTimeline()
				.stream()
				.sorted( Comparator.comparing(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement::getTimestamp))
				.toList();

		log.debug( "Retrieve status history for notification created at={}", createdAt );

		List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement> statusHistory = timelineStatusHistoryDto.getNotificationStatusHistory();

		ModelMapper mapperStatusHistory = new ModelMapper();
		mapperStatusHistory.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, NotificationStatusHistoryElement.class )
				.addMapping( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement::getActiveFrom, NotificationStatusHistoryElement::setActiveFrom );
		mapperStatusHistory.getConfiguration().setMatchingStrategy( MatchingStrategies.STRICT );

		ModelMapper mapperNotification = modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class );

		ModelMapper mapperTimeline = new ModelMapper();
		mapperTimeline.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class )
				.addMapping(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement::getTimestamp, TimelineElement::setTimestamp );
		mapperTimeline.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

		FullSentNotification resultFullSent = notification
				.timeline( timelineList.stream()
						.map( timelineElement -> mapperTimeline.map(timelineElement, TimelineElement.class ) )
						.toList()  )
				.notificationStatusHistory( statusHistory.stream()
						.map( el -> mapperStatusHistory.map( el, NotificationStatusHistoryElement.class ))
						.toList()
				)
				.notificationStatus( NotificationStatus.fromValue( timelineStatusHistoryDto.getNotificationStatus().getValue() ));

		return mapperNotification.map( resultFullSent, InternalNotification.class );
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
			throw new PnNotFoundException("Notification not found" ,"Notification with iun=" + iun + " do not have recipient/delegator=" + userId,
					ERROR_CODE_DELIVERY_USER_ID_NOT_RECIPIENT_OR_DELEGATOR );
		}

		log.info("Send \"notification acknowlwdgement\" event for iun={}", iun);
		Instant createdAt = clock.instant();
		notificationAcknowledgementProducer.sendNotificationViewed( iun, createdAt, recipientIndex );
	}

	private void labelizeGroup(InternalNotification notification, String senderId) {
		String notificationGroup = notification.getGroup();
		// no notification or no sender id
		if (notificationGroup == null || notificationGroup.isEmpty() || senderId == null) {
			return;
		}
		List<PaGroup> groups = pnExternalRegistriesClient.getGroups(senderId);
		if (!groups.isEmpty()) {
			PaGroup group = groups.stream()
					.filter(g -> g.getId().equals(notificationGroup))
					.findAny()
					.orElse(null);
			if (group != null) {
				notification.setGroup(group.getName());
			}
		}
	}

	private void labelizeGroups(ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchResult, String senderId) {
		// no results
		if (searchResult == null) {
			return;
		}
		List<NotificationSearchRow> notifications = searchResult.getResultsPage();
		// no notification or no sender id
		if (notifications == null || notifications.isEmpty() || senderId == null) {
			return;
		}
		List<PaGroup> groups = pnExternalRegistriesClient.getGroups(senderId);
		for (NotificationSearchRow notification : notifications) {
			String notificationGroup = notification.getGroup();
			if (!groups.isEmpty() && notificationGroup != null && !notificationGroup.isEmpty()) {
				PaGroup group = groups.stream()
						.filter(g -> g.getId().equals(notificationGroup))
						.findAny()
						.orElse(null);
				if (group != null) {
					notification.setGroup(group.getName());
				}
			}
		}
	}
}
