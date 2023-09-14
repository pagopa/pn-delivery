package it.pagopa.pn.delivery.svc.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.*;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.*;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaymentStatus;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.svc.authorization.CxType;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
import java.util.stream.IntStream;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.*;
import static it.pagopa.pn.delivery.utils.PgUtils.checkAuthorizationPG;

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
	private final ModelMapper modelMapper;
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
										ModelMapper modelMapper,
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
		this.modelMapper = modelMapper;
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
			} else if (checkAuthorizationPG(recipientType, cxGroups)) {
				log.error("PG {} can not access this resource", searchDto.getSenderReceiverId());
				throw new PnForbiddenException(ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
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
		opaqueFilterId(searchDto);

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
			throw new PnForbiddenException(ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
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

	private void opaqueFilterId(InputSearchNotificationDto searchDto) {
		String searchDtoFilterId = searchDto.getFilterId();
		if ( searchDtoFilterId != null && searchDto.isBySender() && !searchDto.isReceiverIdIsOpaque() ) {
			if ( searchDtoFilterId.length() == 11 ) {
				log.info( "[start] Send request P.Iva to data-vault" );
				searchDto.setOpaqueFilterIdPG( dataVaultClient.ensureRecipientByExternalId( it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType.PG, searchDtoFilterId) );
			}
			if ( searchDtoFilterId.length() == 16 ) {
				log.info( "[start] Send request CF to data-vault" );
				searchDto.setOpaqueFilterIdPF( dataVaultClient.ensureRecipientByExternalId( it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType.PF, searchDtoFilterId) );
				searchDto.setOpaqueFilterIdPG( dataVaultClient.ensureRecipientByExternalId( it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType.PG, searchDtoFilterId) );
			}
			log.info( "[end] Ensured recipient for search" );
			searchDto.setFilterId( searchDtoFilterId );
		} else {
			log.debug( "No filterId or search is by receiver" );
		}
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
		log.info( "START check mandate for receiverId={} and mandateId={}", senderReceiverId, mandateId );
		List<InternalMandateDto> mandates = pnMandateClient.listMandatesByDelegate(senderReceiverId, mandateId, CxTypeAuthFleet.valueOf(recipientType), cxGroups);
		if(!mandates.isEmpty()) {
			boolean validMandate = false;
			for ( InternalMandateDto mandate : mandates ) {
				if (mandate.getDelegator() != null && mandate.getDatefrom() != null && mandate.getMandateId() != null && mandate.getMandateId().equals(mandateId)) {
					validMandate =  adjustSearchDatesAndReceiverAndAllowedPaIds( searchDto, mandate );
					log.info( "Valid mandate for delegate={} mandate={}", senderReceiverId, mandate );
					break;
				}
			}
			if (!validMandate){
				String message = String.format("Unable to find valid mandate for delegate=%s with mandateId=%s", senderReceiverId, mandateId);
				handlePnMandateInvalid(message);
			}
		} else {
			String message = String.format("Unable to find any mandate for delegate=%s with mandateId=%s", senderReceiverId, mandateId);
			handlePnMandateInvalid(message);
		}
		log.info( "END check mandate for receiverId={} and mandateId={}", senderReceiverId, mandateId );
	}

	/**
	 * Adjust search range date and receiver with mandate info
	 *
	 * @param searchDto search input data
	 * @param mandate mandate object
	 * @return true if delegation is valid, false if search is done for a not allowed PA
	 *
	 */
	private boolean adjustSearchDatesAndReceiverAndAllowedPaIds(InputSearchNotificationDto searchDto,
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

		// filtro sugli ID della PA che può visualizzare
		if (StringUtils.hasText(searchDto.getFilterId())
			&& !CollectionUtils.isEmpty(mandate.getVisibilityIds())
			&& !mandate.getVisibilityIds().contains(searchDto.getFilterId()))
		{
			// questo è il caso in cui c'è un filtro per PA e la delega è solo per alcune PA e la PA non è nella delega
			// Da notare che per ora, lato GUI, non c'è possibilità di filtrare per PA, quindi qui dentro non dovrebbe entrarci mai finchè non verrà eventualmente implementata la funzionalità
			// per ora vien lanciato errore (equivale ad una delega non presente), ma in futuro questa condizione potrebbe non essere vera (cioè si preferisce tornare array vuoto senza errori)
			log.warn("user delegate is not allowed too see required paId={} mandateAllowedPaIds={} delegatorId={} delegateId={}", searchDto.getFilterId(), mandate.getVisibilityIds(), mandate.getDelegator(), mandate.getDelegate());
			return false;
		}
		else	// in tutti gli altri casi, non mi interessa fare altri controlli. Se la lista è vuota/nulla non darà luogo a nessun filtro poi.
			searchDto.setMandateAllowedPaIds(mandate.getVisibilityIds());

		log.debug( "Adjust receiverId={}", delegator );
		return  true;
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
		checkDocumentsAvailability(notification, refinementDate , requestBySender);
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
	public InternalNotification getNotificationInformationWithSenderIdCheck(String iun, String senderId, List<String> groups) {
		log.debug( "Retrieve complete notification with sender check by iun={} senderId={} START", iun, senderId );
		InternalNotification notification = getInternalNotification( iun );
		checkSenderId( iun, notification.getSenderPaId(), senderId, notification.getGroup(), groups );
		completeInternalNotificationWithTimeline(iun, true, notification);
		labelizeGroup(notification, senderId);
		return notification;
	}

	private void checkSenderId(String iun, String notificationSenderPaId, String senderId, String notificationGroup, List<String> groups) {
		if ( !notificationSenderPaId.equals( senderId ) )
			throw new PnNotificationNotFoundException(
					String.format("Unable to find notification with iun=%s for senderId=%s", iun, senderId  )
			);
		if ( StringUtils.hasText( notificationGroup ) && !CollectionUtils.isEmpty( groups )
				&& !groups.contains( notificationGroup ) ) {
			throw new PnNotificationNotFoundException(
				String.format("Unable to find notification with iun=%s for senderId=%s in groups=%s", iun, senderId, groups )
			);
		}
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

	protected OffsetDateTime findRefinementDate(List<TimelineElementV20> timeline, String iun) {
		log.debug( "Find refinement date iun={}", iun );
		OffsetDateTime refinementDate = null;
		// cerco elemento timeline con category refinement o notificationView
		Optional<TimelineElementV20> optionalMin = timeline
				.stream()
				.filter(tle -> TimelineElementCategoryV20.REFINEMENT.equals(tle.getCategory() )
						|| TimelineElementCategoryV20.NOTIFICATION_VIEWED.equals( tle.getCategory() ))
				.min( Comparator.comparing(TimelineElementV20::getTimestamp) );
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

	public InternalNotification getNotificationInformation(String senderId, String paProtocolNumber, String idempotenceToken, List<String> groups) {
		Optional<String> optionalRequestId = notificationDao.getRequestId( senderId, paProtocolNumber, idempotenceToken );
		if (optionalRequestId.isEmpty()) {
			String msg = String.format( "Unable to find requestId for senderId=%s paProtocolNumber=%s idempotenceToken=%s", senderId, paProtocolNumber, idempotenceToken );
			throw new PnNotificationNotFoundException(msg);
		}
		String iun = new String( Base64Utils.decodeFromString( optionalRequestId.get() ) );
		return getNotificationInformationWithSenderIdCheck( iun, senderId, groups );
	}

	/**
	 * Get the full detail of a notification by IUN and notify viewed event
	 *
	 * @param iun                	unique identifier of a Notification
	 * @param internalAuthHeader	header cx-*
	 * @param mandateId 	 		id delega (opzionale)
	 * @return Notification
	 */
	public InternalNotification getNotificationAndNotifyViewedEvent(String iun,
																	InternalAuthHeader internalAuthHeader,
																	String mandateId) {
		log.debug("Start getNotificationAndSetViewed for {}", iun);

		String delegatorId = null;
		NotificationViewDelegateInfo delegateInfo = null;
		// cerco prima la notifica in DB, poi controllo la delega, visto che mi serve il paId
		// Il caso più comune infatti è che l'utente abbia il permesso di vedere una certa notifica
		InternalNotification notification = getNotificationInformation(iun);

		if ( StringUtils.hasText( mandateId ) ) {
			delegatorId = checkMandateForNotificationDetail(internalAuthHeader.xPagopaPnCxId(), mandateId, notification.getSenderPaId(), iun, internalAuthHeader.cxType(), internalAuthHeader.xPagopaPnCxGroups());
			delegateInfo = NotificationViewDelegateInfo.builder()
					.mandateId( mandateId )
					.internalId(internalAuthHeader.xPagopaPnCxId())
					.operatorUuid(internalAuthHeader.xPagopaPnUid())
					.delegateType( NotificationViewDelegateInfo.DelegateType.valueOf(internalAuthHeader.cxType()) )
					.build();
		} else if (checkAuthorizationPG(internalAuthHeader.cxType(), internalAuthHeader.xPagopaPnCxGroups())) {
			log.error( "only a PG admin can access this resource" );
			throw new PnForbiddenException(ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
		}

		String recipientId = delegatorId != null ? delegatorId : internalAuthHeader.xPagopaPnCxId();
		int recipientIndex = getRecipientIndexFromRecipientId(notification, recipientId);
		filterTimelinesByRecipient(notification, internalAuthHeader, recipientIndex);
		filterRecipients(notification, internalAuthHeader, recipientIndex);
		notifyNotificationViewedEvent(notification, recipientIndex, delegateInfo);
		return notification;
	}

	private void filterTimelinesByRecipient(InternalNotification internalNotification, InternalAuthHeader internalAuthHeader, int recipientIndex) {
		if (!CxType.PA.name().equals(internalAuthHeader.cxType())) {
			//se il servizio è invocato da un destinatario, devo filtrare la timeline solo per lo specifico destinatario (o suo delegato)
			//filtro (cyType != PA) superfluo poiché attualmente il servizio è invocato solo lato destinatario
			List<TimelineElementV20> timeline = internalNotification.getTimeline();
			log.debug("Timelines size before filter: {}", timeline.size());

			List<TimelineElementV20> filteredTimelineElements = timeline.stream().filter(timelineElement -> timelineElement.getDetails() == null ||
							timelineElement.getDetails().getRecIndex() == null ||
							timelineElement.getDetails().getRecIndex() == recipientIndex)
					.toList();

			log.debug("Timelines size after filter: {}", filteredTimelineElements.size());
			internalNotification.setTimeline(filteredTimelineElements);
		}
	}

	private void filterRecipients(InternalNotification internalNotification, InternalAuthHeader internalAuthHeader, int recipientIndex) {
		if (!CxType.PA.name().equals(internalAuthHeader.cxType())) {
			//se il servizio è invocato da un destinatario (o suo delegato), devo vedere tutti i dati in chiaro solo per lo specifico destinatario
			//filtro (cyType != PA) superfluo poiché attualmente il servizio è invocato solo lato destinatario

			//"pulisco gli altri destinatari"
			var filteredNotificationRecipients = new ArrayList<NotificationRecipient>();
			for(int i = 0; i< internalNotification.getRecipients().size(); i ++) {
				NotificationRecipient recipient = internalNotification.getRecipients().get(i);
				if(i != recipientIndex) {
					recipient = NotificationRecipient.builder()
							.recipientType(recipient.getRecipientType())
							.internalId(recipient.getInternalId())
							.build();
				}
				filteredNotificationRecipients.add(recipient);
			}

			internalNotification.setRecipients(filteredNotificationRecipients);
		}
	}

	private String checkMandateForNotificationDetail(String userId, String mandateId, String paId, String iun, String recipientType, List<String> cxGroups) {
		CxTypeAuthFleet cxTypeAuthFleet = StringUtils.hasText(recipientType) ? CxTypeAuthFleet.valueOf(recipientType) : null;
		List<InternalMandateDto> mandates = pnMandateClient.listMandatesByDelegate(userId, mandateId, cxTypeAuthFleet, cxGroups);
		if(!mandates.isEmpty()) {

			for ( InternalMandateDto mandate : mandates ) {
				String delegatorId = evaluateMandateAndRetrieveDelegatorId(userId, mandateId, paId, iun, mandate);
				if (delegatorId != null)
					return delegatorId;
			}
		}

		String message = String.format("Unable to find any mandate for notification detail for delegate=%s with mandateId=%s iun=%s", userId, mandateId, iun);
		handlePnMandateInvalid(message);
		return null;
	}

	private String evaluateMandateAndRetrieveDelegatorId(String userId, String mandateId, String paId, String iun, InternalMandateDto mandate){
		if (mandate.getDelegator() != null && mandate.getMandateId() != null && mandate.getMandateId().equals(mandateId)) {

			if( !CollectionUtils.isEmpty(mandate.getVisibilityIds()) ) {
				boolean isPaIdInVisibilityPa = mandate.getVisibilityIds().stream().anyMatch(
						paId::equals
				);

				if( !isPaIdInVisibilityPa ){
					String message = String.format("Unable to find valid mandate for notification detail, paNotificationId=%s is not in visibility pa id for mandate" +
							"- iun=%s delegate=%s with mandateId=%s", paId, iun, userId, mandateId);
					log.warn(message);
					return null;
				}
			}

			log.info( "Valid mandate for notification detail for delegate={}", userId );
			return mandate.getDelegator();
		}

		return null;
	}

	private void handlePnMandateInvalid(String message) {
		log.error(message);
		throw new PnMandateNotFoundException(message);
	}

	private void checkDocumentsAvailability(InternalNotification notification, OffsetDateTime refinementDate, boolean requestBySender) {
		log.debug( "Check if documents are available for iun={}", notification.getIun() );
		notification.setDocumentsAvailable( true );
		if ( requestBySender || !isNotificationCancelled(notification) ) {
			checkDocumentsRemove(notification, refinementDate);
		} else {
			log.debug("Documents not more available for iun={} because is cancelled", notification.getIun());
			notification.setDocumentsAvailable( false );
			// i documenti vanno rimossi solo se trascorso il tempo
			checkDocumentsRemove(notification, refinementDate);
		}
	}

	private void checkDocumentsRemove(InternalNotification notification, OffsetDateTime refinementDate) {
		log.debug( "Check if documents should be removed for iun={}", notification.getIun() );
		if ( refinementDate != null ) {
			long daysBetween = ChronoUnit.DAYS.between( refinementDate.toInstant().truncatedTo( ChronoUnit.DAYS ),
					clock.instant().truncatedTo( ChronoUnit.DAYS ) );
			if ( daysBetween > Long.parseLong( cfg.getMaxDocumentsAvailableDays() ) ) {
				log.debug("Documents not more available for iun={} from={}", notification.getIun(), refinementDate);
				removeDocuments( notification );
			}
		}
	}

	private void removeDocuments(InternalNotification notification) {
		notification.setDocumentsAvailable( false );
		notification.setDocuments( Collections.emptyList() );
		for ( NotificationRecipient recipient : notification.getRecipients() ) {
			NotificationPaymentInfo payment = recipient.getPayment();
			if ( payment != null ) {
				payment.setPagoPaForm( null );
			}
		}
	}

	public InternalNotification enrichWithTimelineAndStatusHistory(String iun, InternalNotification notification) {
		log.debug( "Retrieve timeline for iun={}", iun );
		int numberOfRecipients = notification.getRecipients().size();
		OffsetDateTime createdAt =  notification.getSentAt();

		NotificationHistoryResponse timelineStatusHistoryDto =  pnDeliveryPushClient.getTimelineAndStatusHistory(iun,numberOfRecipients, createdAt);


		// la lista arriva già ordinata correttamente
		var timelineList = timelineStatusHistoryDto.getTimeline();

		log.debug( "Retrieve status history for notification created at={}", createdAt );

		List<it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElement> statusHistory = timelineStatusHistoryDto.getNotificationStatusHistory();

		FullSentNotificationV20 resultFullSent = notification
				.timeline( timelineList.stream()
						.map( timelineElement -> modelMapper.map(timelineElement, TimelineElementV20.class ) )
						.toList()  )
				.notificationStatusHistory( statusHistory.stream()
						.map( el -> modelMapper.map( el, NotificationStatusHistoryElement.class ))
						.toList()
				)
				.notificationStatus( NotificationStatus.fromValue( timelineStatusHistoryDto.getNotificationStatus().getValue() ));

		return modelMapper.map( resultFullSent, InternalNotification.class );
	}


	private void notifyNotificationViewedEvent(InternalNotification notification, int recipientIndex, NotificationViewDelegateInfo delegateInfo) {
		String iun = notification.getIun();
		log.info("Send \"notification acknowlwdgement\" event for iun={}", iun);
		Instant createdAt = clock.instant();
		notificationAcknowledgementProducer.sendNotificationViewed( iun, createdAt, recipientIndex, delegateInfo );
	}

	private void labelizeGroup(InternalNotification notification, String senderId) {
		String notificationGroup = notification.getGroup();
		// no notification or no sender id
		if (notificationGroup == null || notificationGroup.isEmpty() || senderId == null) {
			return;
		}
		List<PaGroup> groups = pnExternalRegistriesClient.getGroups(senderId, false);
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
		List<PaGroup> groups = pnExternalRegistriesClient.getGroups(senderId, false);
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

	private int getRecipientIndexFromRecipientId(InternalNotification internalNotification, String recipientId) {
		int recIndex = IntStream.range(0, internalNotification.getRecipientIds().size())
				.filter(i -> internalNotification.getRecipientIds().get(i).equals(recipientId))
				.findFirst().orElse(-1);

		if( recIndex == -1 ) {
			log.debug("Recipient not found for iun={} and recipientId={} ", internalNotification.getIun(), recipientId );
		throw new PnNotFoundException("Notification not found" ,"Notification with iun=" +
					internalNotification.getIun() + " do not have recipient/delegator=" + recipientId,
					ERROR_CODE_DELIVERY_USER_ID_NOT_RECIPIENT_OR_DELEGATOR );
		}

		return recIndex;
	}

	public void checkIfNotificationIsNotCancelled(String iun) {
		// recuperare tutta la timeline per controllare lo stato di richiesta è inefficente
		if(isNotificationCancelled(getNotificationInformation(iun))) {
			throw new PnNotificationNotFoundException(String.format("Notification with iun: %s has a request for cancellation", iun));
		}
 	}

	public boolean isNotificationCancelled(InternalNotification notification) {
		var cancellationRequestCategory = TimelineElementCategoryV20.NOTIFICATION_CANCELLATION_REQUEST;
		Optional<TimelineElementV20> cancellationRequestTimeline = notification.getTimeline().stream()
				.filter(timelineElement -> cancellationRequestCategory.toString().equals(timelineElement.getCategory().toString()))
				.findFirst();
		boolean cancellationTimelineIsPresent = cancellationRequestTimeline.isPresent();
		if(cancellationTimelineIsPresent) {
			log.warn("Notification with iun: {} has a request for cancellation", notification.getIun());
		}
		return cancellationTimelineIsPresent;
	}

}
