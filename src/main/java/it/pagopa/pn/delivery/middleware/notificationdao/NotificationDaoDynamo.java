package it.pagopa.pn.delivery.middleware.notificationdao;



import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.AddressDto;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.AnalogDomicile;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDocument;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Component
@Slf4j
public class NotificationDaoDynamo implements NotificationDao {

	private final NotificationEntityDao entityDao;
	private final NotificationMetadataEntityDao metadataEntityDao;
	private final DtoToEntityNotificationMapper dto2entityMapper;
	private final EntityToDtoNotificationMapper entity2DtoMapper;
	private final PnDataVaultClientImpl pnDataVaultClient;

	public NotificationDaoDynamo(
			NotificationEntityDao entityDao,
			NotificationMetadataEntityDao metadataEntityDao, DtoToEntityNotificationMapper dto2entityMapper,
			EntityToDtoNotificationMapper entity2DtoMapper, PnDataVaultClientImpl pnDataVaultClient) {
		this.entityDao = entityDao;
		this.metadataEntityDao = metadataEntityDao;
		this.dto2entityMapper = dto2entityMapper;
		this.entity2DtoMapper = entity2DtoMapper;
		this.pnDataVaultClient = pnDataVaultClient;
	}

	@Override
	public void addNotification(InternalNotification internalNotification) throws IdConflictException {

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
				.payment( recipient.getPayment() )
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
				.state( notificationPhysicalAddress.getForeignState() );
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
			handleRecipients(daoResult);

			handleDocuments(daoResult);
		}
		return daoResult;
	}

	private void handleRecipients(Optional<InternalNotification> daoResult) {
		List<NotificationRecipient> daoNotificationRecipientList = daoResult.get().getRecipients();

		List<String> opaqueIds = daoNotificationRecipientList.stream()
				.map(NotificationRecipient::getTaxId)
				.collect(Collectors.toList());

		List<BaseRecipientDto> baseRecipientDtoList =
				pnDataVaultClient.getRecipientDenominationByInternalId(
						opaqueIds
						);

		List<NotificationRecipientAddressesDto> notificationRecipientAddressesDtoList = pnDataVaultClient.getNotificationAddressesByIun( daoResult.get().getIun() );
		List<String> opaqueRecipientsIds = new ArrayList<>();

		int recipientIndex = 0;
		for ( NotificationRecipient recipient : daoNotificationRecipientList ) {
			String opaqueTaxId = recipient.getTaxId();
			opaqueRecipientsIds.add( opaqueTaxId );

			BaseRecipientDto baseRec =
					recipientIndex < baseRecipientDtoList.size()
					? baseRecipientDtoList.get( recipientIndex ) : null;
			NotificationRecipientAddressesDto clearDataAddresses =
					recipientIndex < notificationRecipientAddressesDtoList.size()
					? notificationRecipientAddressesDtoList.get( recipientIndex ) : null;

			if ( baseRec != null) {
				recipient.setTaxId(baseRec.getTaxId());
				recipient.setDenomination(baseRec.getDenomination());
			}
			else {
				log.error( "Unable to find any recipient info from data-vault for recipient={}", opaqueTaxId );
			}

			if ( clearDataAddresses != null ) {
				recipient.setDigitalDomicile( setNotificationDigitalAddress( clearDataAddresses.getDigitalAddress() ));
				recipient.setPhysicalAddress( setNotificationPhysicalAddress( clearDataAddresses.getPhysicalAddress() ) );
			} else {
				log.error( "Unable to find any recipient addresses from data-vault for recipient={}", opaqueTaxId );
			}

			recipientIndex += 1;
		}
		daoResult.get().setRecipientIds( opaqueRecipientsIds );
	}

	private void handleDocuments(Optional<InternalNotification> daoResult) {
		Integer docIdx = 0; 
		if(daoResult.get() != null && daoResult.get().getDocuments() != null ) {
			for (NotificationDocument doc : daoResult.get().getDocuments()) {
				doc.setDocIdx(docIdx.toString());
				docIdx++;
			}			
		}		
	}


	private NotificationDigitalAddress setNotificationDigitalAddress( AddressDto addressDto ) {
		return addressDto == null ? null : NotificationDigitalAddress.builder()
				.type( NotificationDigitalAddress.TypeEnum.PEC )
				.address( addressDto.getValue() )
				.build();
	}

	private NotificationPhysicalAddress setNotificationPhysicalAddress( AnalogDomicile analogDomicile ) {
		return analogDomicile == null ? null : NotificationPhysicalAddress.builder()
				.foreignState( analogDomicile.getState() )
				.address( analogDomicile.getAddress() )
				.addressDetails( analogDomicile.getAddressDetails() )
				.at( analogDomicile.getAt() )
				.zip( analogDomicile.getCap() )
				.province( analogDomicile.getProvince() )
				.municipality( analogDomicile.getMunicipality() )
				.build();
	}

	@Override
	public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchForOneMonth(InputSearchNotificationDto inputSearchNotificationDto, String indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {
		return this.metadataEntityDao.searchForOneMonth( inputSearchNotificationDto, indexName, partitionValue, size, lastEvaluatedKey );
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
