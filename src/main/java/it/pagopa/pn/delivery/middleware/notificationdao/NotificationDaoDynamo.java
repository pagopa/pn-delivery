package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.svc.search.IndexNameAndPartitions;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NotificationDaoDynamo implements NotificationDao {

	private final NotificationEntityDao entityDao;
	private final NotificationMetadataEntityDao metadataEntityDao;
	private final NotificationDelegationMetadataEntityDao delegationMetadataEntityDao;
	private final DtoToEntityNotificationMapper dto2entityMapper;
	private final EntityToDtoNotificationMapper entity2DtoMapper;
	private final PnDataVaultClientImpl pnDataVaultClient;

	public NotificationDaoDynamo(NotificationEntityDao entityDao,
								 NotificationMetadataEntityDao metadataEntityDao,
								 NotificationDelegationMetadataEntityDao delegationMetadataEntityDao,
								 DtoToEntityNotificationMapper dto2entityMapper,
								 EntityToDtoNotificationMapper entity2DtoMapper,
								 PnDataVaultClientImpl pnDataVaultClient) {
		this.entityDao = entityDao;
		this.metadataEntityDao = metadataEntityDao;
		this.delegationMetadataEntityDao = delegationMetadataEntityDao;
		this.dto2entityMapper = dto2entityMapper;
		this.entity2DtoMapper = entity2DtoMapper;
		this.pnDataVaultClient = pnDataVaultClient;
	}

	@Override
	public void addNotification(InternalNotification internalNotification  ) throws PnIdConflictException {

		List<NotificationRecipientAddressesDto> recipientAddressesDtoList = new ArrayList<>();
		List<NotificationRecipient> cleanedRecipientList = new ArrayList<>();
		for ( NotificationRecipient recipient  : internalNotification.getRecipients()) {
			String opaqueTaxId = pnDataVaultClient.ensureRecipientByExternalId( RecipientType.fromValue( recipient.getRecipientType().getValue() ), recipient.getTaxId() );
			recipient.setTaxId( opaqueTaxId );
			NotificationRecipientAddressesDto recipientAddressesDto = new NotificationRecipientAddressesDto()
					.denomination( recipient.getDenomination() )
					.digitalAddress( createDigitalDomicile( recipient.getDigitalDomicile() ) )
					.physicalAddress( createAnalogDomicile( recipient.getPhysicalAddress() ) );
			recipientAddressesDtoList.add( recipientAddressesDto );
			cleanedRecipientList.add( removeConfidantialInfo( recipient ) );
		}

		pnDataVaultClient.updateNotificationAddressesByIun( internalNotification.getIun(), recipientAddressesDtoList );
		internalNotification.setRecipients( cleanedRecipientList );

		NotificationEntity entity = dto2entityMapper.dto2Entity( internalNotification );

		entityDao.putIfAbsent( entity );
	}

	private NotificationRecipient removeConfidantialInfo(NotificationRecipient recipient) {
		return NotificationRecipient.builder()
				.recipientType( recipient.getRecipientType() )
				.taxId( recipient.getTaxId() )
				.payments( recipient.getPayments() )
				.build();
	}

	private AnalogDomicile createAnalogDomicile(NotificationPhysicalAddress notificationPhysicalAddress ) {
		return notificationPhysicalAddress == null ? null : new AnalogDomicile()
				.address( notificationPhysicalAddress.getAddress() )
				.addressDetails( notificationPhysicalAddress.getAddressDetails() )
				.at( notificationPhysicalAddress.getAt() )
				.cap( notificationPhysicalAddress.getZip() )
				.municipality( notificationPhysicalAddress.getMunicipality() )
				.province( notificationPhysicalAddress.getProvince() )
				.state( notificationPhysicalAddress.getForeignState() )
				.municipalityDetails( notificationPhysicalAddress.getMunicipalityDetails() );
	}

	private AddressDto createDigitalDomicile(NotificationDigitalAddress digitalAddress) {
		return digitalAddress == null ? null : new AddressDto()
				.value( digitalAddress.getAddress() );
	}

	@Override
	public Optional<InternalNotification> getNotificationByIun(String iun) {
		Key keyToSearch = Key.builder()
				.partitionValue(iun)
				.build();
		Optional<InternalNotification> daoResult = entityDao.get( keyToSearch )
				.map( entity2DtoMapper::entity2Dto );

		if(daoResult.isPresent()) {
			handleRecipients(daoResult.get());
			handleDocuments(daoResult.get());
		}
		return daoResult;
	}

	@Override
	public Optional<String> getRequestId( String senderId, String paProtocolNumber, String idempotenceToken ) {
		Key keyToSearch = Key.builder()
				.partitionValue( senderId + "##" + paProtocolNumber + "##" + idempotenceToken )
				.build();

		return entityDao.get( keyToSearch )
				.map(NotificationEntity::getRequestId);
	}

	private void handleRecipients(InternalNotification daoResult) {
		List<NotificationRecipient> daoNotificationRecipientList = daoResult.getRecipients();

		Set<String> opaqueIds = daoNotificationRecipientList.stream()
				.map(NotificationRecipient::getInternalId)
				.collect(Collectors.toSet());

		List<BaseRecipientDto> baseRecipientDtoList =
				pnDataVaultClient.getRecipientDenominationByInternalId( new ArrayList<>(opaqueIds) );

		List<NotificationRecipientAddressesDto> notificationRecipientAddressesDtoList = pnDataVaultClient.getNotificationAddressesByIun( daoResult.getIun() );
		List<String> opaqueRecipientsIds = new ArrayList<>();
		int recipientIndex = 0;
		for ( NotificationRecipient recipient : daoNotificationRecipientList ) {
			String opaqueTaxId = recipient.getInternalId();
			opaqueRecipientsIds.add( opaqueTaxId );

			BaseRecipientDto baseRec = baseRecipientDtoList.stream()
					.filter( el ->Objects.equals( opaqueTaxId, el.getInternalId()) )
					.findAny()
					.orElse( null );

			NotificationRecipientAddressesDto clearDataAddresses =
					recipientIndex < notificationRecipientAddressesDtoList.size()
							? notificationRecipientAddressesDtoList.get( recipientIndex ) : null;


			if ( baseRec != null) {
				recipient.setTaxId(baseRec.getTaxId());
			}
			else {
				log.error( "Unable to find any recipient info from data-vault for recipient={}", opaqueTaxId );
			}

			if ( clearDataAddresses != null ) {
				recipient.setDenomination(clearDataAddresses.getDenomination());
				recipient.setDigitalDomicile( setNotificationDigitalAddress( clearDataAddresses.getDigitalAddress() ));
				recipient.setPhysicalAddress( setNotificationPhysicalAddress( clearDataAddresses.getPhysicalAddress() ) );
			} else {
				log.error( "Unable to find any recipient addresses from data-vault for recipient={}", opaqueTaxId );
			}
			recipientIndex += 1;
		}
		daoResult.setRecipientIds( opaqueRecipientsIds );
	}

	private void handleDocuments(InternalNotification daoResult) {
		int docIdx = 0;
		for (NotificationDocument doc : daoResult.getDocuments()) {
			doc.setDocIdx(Integer.toString(docIdx));
			docIdx++;
		}
	}

	private NotificationDigitalAddress setNotificationDigitalAddress(AddressDto addressDto ) {
		return addressDto == null ? null : NotificationDigitalAddress.builder()
				.type( it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress.TypeEnum.PEC )
				.address( addressDto.getValue() )
				.build();
	}

	private NotificationPhysicalAddress setNotificationPhysicalAddress(AnalogDomicile analogDomicile ) {
		return analogDomicile == null ? null : NotificationPhysicalAddress.builder()
				.foreignState( analogDomicile.getState() )
				.address( analogDomicile.getAddress() )
				.addressDetails( analogDomicile.getAddressDetails() )
				.at( analogDomicile.getAt() )
				.zip( analogDomicile.getCap() )
				.province( analogDomicile.getProvince() )
				.municipality( analogDomicile.getMunicipality() )
				.municipalityDetails(analogDomicile.getMunicipalityDetails() )
				.build();
	}

	@Override
	public PageSearchTrunk<NotificationMetadataEntity> searchForOneMonth(InputSearchNotificationDto inputSearchNotificationDto, String indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {
		return this.metadataEntityDao.searchForOneMonth( inputSearchNotificationDto, indexName, partitionValue, size, lastEvaluatedKey );
	}

	@Override
	public PageSearchTrunk<NotificationDelegationMetadataEntity> searchDelegatedForOneMonth(InputSearchNotificationDelegatedDto searchDto,
																							IndexNameAndPartitions.SearchIndexEnum indexName,
																							String partitionValue,
																							int size,
																							PnLastEvaluatedKey lastEvaluatedKey) {
		return delegationMetadataEntityDao.searchForOneMonth(searchDto, indexName, partitionValue, size, lastEvaluatedKey);
	}

	@Override
	public PageSearchTrunk<NotificationMetadataEntity> searchByIUN(InputSearchNotificationDto inputSearchNotificationDto) {
		log.debug("searchByIUN iun={}", inputSearchNotificationDto.getIunMatch());

		String iun = inputSearchNotificationDto.getIunMatch();

		Key keyToSearch = Key.builder()
				.partitionValue(iun)
				.build();
		Optional<NotificationEntity> daoResult = entityDao.get( keyToSearch );

		if(daoResult.isPresent()) {
			// controllo se lo IUN richiesto fa parte di una PA su cui ho il permesso.
			// NB: mandateAllowedPaIds sarà popolato solo se c'è una delega, e se tale delega prevede una visibilità solo per alcune PA
			log.debug("notification found, proceeding with check if user is allowed for this PA allowedPaIds={}", inputSearchNotificationDto.getMandateAllowedPaIds());
			if (CollectionUtils.isEmpty(inputSearchNotificationDto.getMandateAllowedPaIds()) || inputSearchNotificationDto.getMandateAllowedPaIds().contains(daoResult.get().getSenderPaId()))
			{
				log.debug("notification found and allowed, proceeding with retrieve by recipient");
				String recipientId = daoResult.get().getRecipients().get(0).getRecipientId();
				return this.metadataEntityDao.searchByIun( inputSearchNotificationDto,  iun + "##" + recipientId, daoResult.get().getSentAt().toString());
			}
			else
			{
				log.info("user is not allowed to see PA paId={} iun={}", daoResult.get().getSenderPaId(), iun);
				return new PageSearchTrunk<>();
			}
		}

		return new PageSearchTrunk<>();
	}

	@Override
	public Page<NotificationDelegationMetadataEntity> searchByPk(InputSearchNotificationDelegatedDto searchDto) {
		return delegationMetadataEntityDao.searchExactNotification(searchDto);
	}


	Predicate<String> buildRegexpPredicate(String subjectRegExp) {
		Predicate<String> matchSubject;
		if (subjectRegExp != null) {
			matchSubject = Objects::nonNull;
			matchSubject = matchSubject.and(Pattern.compile("^" + subjectRegExp + "$").asMatchPredicate());
		} else {
			matchSubject = x -> true;
		}
		return matchSubject;
	}
}
