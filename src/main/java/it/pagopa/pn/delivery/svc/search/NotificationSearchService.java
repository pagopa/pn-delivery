package it.pagopa.pn.delivery.svc.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnForbiddenException;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.models.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategoryV28;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV28;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_INVALID_MANDATE_COMMUNICATION_TYPE;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_LAST_EVALUATED_KEY;
import static it.pagopa.pn.delivery.utils.PgUtils.checkAuthorizationPG;

@Service
@Slf4j
public class NotificationSearchService {

	private static final Instant PN_EPOCH = Instant.ofEpochSecond( 1651399200 ); // 2022-05-01T12:00:00.000 GMT+2:00
    private final PnMandateClientImpl pnMandateClient;
	private final PnDataVaultClientImpl dataVaultClient;
	private final PnExternalRegistriesClientImpl pnExternalRegistriesClient;
    private final NotificationSearchFactory notificationSearchFactory;
	private final RefinementLocalDate refinementLocalDateUtils;


    @Autowired
	public NotificationSearchService(PnMandateClientImpl pnMandateClient,
                                     PnDataVaultClientImpl dataVaultClient,
                                     PnExternalRegistriesClientImpl pnExternalRegistriesClient,
                                     NotificationSearchFactory notificationSearchFactory,
                                     RefinementLocalDate refinementLocalDateUtils) {
        this.pnMandateClient = pnMandateClient;
		this.dataVaultClient = dataVaultClient;
		this.pnExternalRegistriesClient = pnExternalRegistriesClient;
        this.notificationSearchFactory = notificationSearchFactory;
		this.refinementLocalDateUtils = refinementLocalDateUtils;
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

        if ( searchDto.isByCampaign() ) {
            // la ricerca per campagna riguarda per definizione notifiche bonarie: si forza INFORMAL.
            // Non si applica il default LEGAL, né il ramo deleghe (mandate), né l'auth PG del destinatario:
            // è un flusso lato mittente sugli indici di campagna.
            searchDto.setCommunicationType( NotificationSearchCommunicationType.INFORMAL );
        } else {
            // default applicativo SRS: in assenza di tipologia di comunicazione si filtra sulle sole notifiche legali per gestire client non aggiornati
            if ( searchDto.getCommunicationType() == null ) {
                searchDto.setCommunicationType( NotificationSearchCommunicationType.LEGAL );
            }

            if ( !searchDto.isBySender() ) {
                log.debug( "Search from receiver" );
                String mandateId = searchDto.getMandateId();
                if ( StringUtils.hasText( mandateId )) {
					if ( NotificationSearchCommunicationType.INFORMAL.equals(searchDto.getCommunicationType()) ) {
						throw new PnBadRequestException(
								"Invalid communication type for mandate search",
								"INFORMAL communication type is not supported when mandateId is provided",
								ERROR_CODE_DELIVERY_INVALID_MANDATE_COMMUNICATION_TYPE);
					}
					if ( NotificationSearchCommunicationType.ALL.equals(searchDto.getCommunicationType()) ) {
						// se il client non specifica la tipologia di comunicazione, si forza LEGAL per la ricerca con mandateId
						searchDto.setCommunicationType( NotificationSearchCommunicationType.LEGAL );
					}
                    checkMandate(searchDto, mandateId, recipientType, cxGroups);
                } else if (checkAuthorizationPG(recipientType, cxGroups)) {
                    log.error("PG {} can not access this resource", searchDto.getSenderReceiverId());
                    throw new PnForbiddenException(ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
                }
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
		// il filterId (recipientId) arriva in chiaro (CF/P.IVA) sia dal mittente "legal" sia dal
		// mittente di una campagna bonaria: in entrambi i casi va anonimizzato tramite data-vault
		// prima di essere usato come chiave di ricerca
		boolean requiresOpaqueConversion = searchDto.isBySender() || searchDto.isByCampaign();
		if ( searchDtoFilterId != null && requiresOpaqueConversion && !searchDto.isReceiverIdIsOpaque() ) {
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

	private void handlePnMandateInvalid(String message) {
		log.error(message);
		throw new PnMandateNotFoundException(message);
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

	protected OffsetDateTime findRefinementDate(List<TimelineElementV28> timeline, String iun) {
		log.debug( "Find refinement date iun={}", iun );
		OffsetDateTime refinementDate = null;
		// cerco elemento timeline con category refinement o notificationView
		Optional<TimelineElementV28> optionalMin = timeline
				.stream()
				.filter(tle -> TimelineElementCategoryV28.REFINEMENT.equals(tle.getCategory() )
						|| TimelineElementCategoryV28.NOTIFICATION_VIEWED.equals( tle.getCategory() ))
				.min( Comparator.comparing(TimelineElementV28::getTimestamp) );
		// se trovo la data di perfezionamento della notifica
		if (optionalMin.isPresent()) {
			refinementDate = refinementLocalDateUtils.setLocalRefinementDate(optionalMin.get());
		} else {
			log.debug( "Notification iun={} not perfected", iun );
		}
		return refinementDate;
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
                groups.stream()
                        .filter(g -> Objects.requireNonNull(g.getId()).equals(notificationGroup))
                        .findAny().ifPresent(group -> notification.setGroup(group.getName()));
            }
		}
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
}
