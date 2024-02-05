package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.config.SendActiveParameterConsumer;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.model.SaveF24Item;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.model.SaveF24Request;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequestV23;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationResponse;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.pnf24.PnF24ClientImpl;
import lombok.CustomLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_INVALIDPARAMETER_GROUP;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_SEND_IS_DISABLED;
import static it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy.DELIVERY_MODE;

@Service
@CustomLog
public class NotificationReceiverService {
	public static final String LATEST_NOTIFICATION_VERSION = "2.3";

	private final Clock clock;
	private final NotificationDao notificationDao;
	private final NotificationReceiverValidator validator;
	private final ModelMapper modelMapper;

	private final SendActiveParameterConsumer parameterConsumer;

	private final PnExternalRegistriesClientImpl pnExternalRegistriesClient;
	private final PnF24ClientImpl f24Client;

	private final IunGenerator iunGenerator = new IunGenerator();

	private final PnDeliveryConfigs cfg;

	@Autowired
	public NotificationReceiverService(
			Clock clock,
			NotificationDao notificationDao,
			NotificationReceiverValidator validator,
			ModelMapper modelMapper,
			SendActiveParameterConsumer parameterConsumer,
			PnExternalRegistriesClientImpl pnExternalRegistriesClient,
			PnF24ClientImpl f24Client,
			PnDeliveryConfigs cfg) {
		this.clock = clock;
		this.notificationDao = notificationDao;
		this.validator = validator;
		this.modelMapper = modelMapper;
		this.parameterConsumer = parameterConsumer;
		this.pnExternalRegistriesClient = pnExternalRegistriesClient;
		this.f24Client = f24Client;
		this.cfg = cfg;
	}

	/**
	 * Store metadata and documents about a new notification request
	 *
	 * @param xPagopaPnCxId Public Administration id
	 * @param newNotificationRequest Public Administration notification request that PN have to forward to
	 *                     one or more recipient
	 * @param xPagopaPnCxGroups PA Group id List
	 * @return A model with the generated IUN and the paNotificationId sent by the
	 *         Public Administration
	 */
	public NewNotificationResponse receiveNotification(
			String xPagopaPnCxId,
			NewNotificationRequestV23 newNotificationRequest,
			String xPagopaPnSrcCh,
			String xPagopaPnSrcChDetails,
			List<String> xPagopaPnCxGroups,
			String xPagopaPnNotificationVersion
	) throws PnIdConflictException {
		log.info("New notification storing START");

		if ( Boolean.FALSE.equals( parameterConsumer.isSendActive( newNotificationRequest.getSenderTaxId() ) )) {
			throw new PnBadRequestException( "SEND non é abilitata", "Comunicazione notifiche disabilitata", ERROR_CODE_DELIVERY_SEND_IS_DISABLED,
					"Piattaforma Notifiche non é abilitata alla comunicazione di notifiche." );
		}

		log.debug("New notification storing START paProtocolNumber={} idempotenceToken={}",
				newNotificationRequest.getPaProtocolNumber(), newNotificationRequest.getIdempotenceToken());
		log.logChecking("New notification request validation process");
		validator.checkNewNotificationRequestBeforeInsertAndThrow(newNotificationRequest);
		log.debug("Validation OK for paProtocolNumber={}", newNotificationRequest.getPaProtocolNumber() );
		log.logCheckingOutcome("New notification request validation process", true, "");
		String notificationGroup = newNotificationRequest.getGroup();
		checkGroup(xPagopaPnCxId, notificationGroup, xPagopaPnCxGroups);

		setPagoPaIntMode(newNotificationRequest);

		InternalNotification internalNotification = modelMapper.map(newNotificationRequest, InternalNotification.class);

		internalNotification.setSenderPaId( xPagopaPnCxId );
		internalNotification.setSourceChannel( xPagopaPnSrcCh );
		internalNotification.setSourceChannelDetails(xPagopaPnSrcChDetails);
		internalNotification.setVersion( StringUtils.hasText( xPagopaPnNotificationVersion ) ? xPagopaPnNotificationVersion : LATEST_NOTIFICATION_VERSION );

		SaveF24Request saveF24Request = new SaveF24Request();
		List<SaveF24Item> saveF24Items = IntStream.range(0, internalNotification.getRecipients().size())
				.boxed()
				.flatMap(recipientIndex -> {
					NotificationRecipient notificationRecipient = internalNotification.getRecipients().get(recipientIndex);
					if (!CollectionUtils.isEmpty(notificationRecipient.getPayments())) {
						return IntStream.range(0, notificationRecipient.getPayments().size())
								.boxed()
								.flatMap(paymentIndex -> {
									NotificationPaymentInfo notificationPaymentInfo = notificationRecipient.getPayments().get(paymentIndex);
									if (notificationPaymentInfo.getF24() != null) {
										SaveF24Item saveF24Item = constructF24Item(notificationPaymentInfo, recipientIndex, paymentIndex);
										return Stream.of(saveF24Item);
									} else {
										return Stream.empty();
									}
								});
					}
					else{
						return Stream.empty();
					}
				})
				.toList();

		String iun = generateIun(internalNotification);
		saveF24Request.setF24Items(saveF24Items);
		saveF24Request.setId(internalNotification.getIun());

		if(!saveF24Items.isEmpty()){
			f24Client.saveMetadata(this.cfg.getF24CxId(), internalNotification.getIun(), saveF24Request);
		}

		doSaveWithRethrow(internalNotification);

		NewNotificationResponse response = generateResponse(internalNotification, iun);

		log.info("New notification storing END {}", response);
		return response;
	}

	private SaveF24Item constructF24Item(NotificationPaymentInfo notificationPaymentInfo, Integer recipientIndex, Integer paymentIndex) {
		SaveF24Item saveF24Item = new SaveF24Item();
		saveF24Item.setApplyCost(notificationPaymentInfo.getF24().isApplyCost());
		saveF24Item.setSha256(notificationPaymentInfo.getF24().getMetadataAttachment().getDigests() != null ? notificationPaymentInfo.getF24().getMetadataAttachment().getDigests().getSha256() : null);
		saveF24Item.setFileKey(notificationPaymentInfo.getF24().getMetadataAttachment().getRef() != null ? notificationPaymentInfo.getF24().getMetadataAttachment().getRef().getKey() : null);
		saveF24Item.setPathTokens(List.of(Integer.toString(recipientIndex), Integer.toString(paymentIndex)));
		return saveF24Item;
	}

	private void checkGroup(String senderId, String notificationGroup, List<String> xPagopaPnCxGroups) {

		if( StringUtils.hasText( notificationGroup ) ) {

			List<PaGroup> paGroups = pnExternalRegistriesClient.getGroups( senderId, true );
			PaGroup paGroup = paGroups.stream().filter(elem -> {
				assert elem.getId() != null;
				return elem.getId().equals(notificationGroup);
			}).findAny().orElse(null);

			if( paGroup == null ){
				String logMessage = String.format("Group=%s not present or suspended/deleted in pa_groups=%s", notificationGroup, paGroup);
				throw new PnInvalidInputException(ERROR_CODE_DELIVERY_INVALIDPARAMETER_GROUP, notificationGroup, logMessage);
			}

			if( !CollectionUtils.isEmpty( xPagopaPnCxGroups ) && !xPagopaPnCxGroups.contains(notificationGroup)){
				String logMessage = String.format("Group=%s not present in cx_groups=%s", notificationGroup, xPagopaPnCxGroups);
				throw new PnInvalidInputException(ERROR_CODE_DELIVERY_INVALIDPARAMETER_GROUP, notificationGroup, logMessage);
			}
		} else {
			if( !CollectionUtils.isEmpty( xPagopaPnCxGroups ) ) {
				String logMessage = String.format( "Specify a group in cx_groups=%s", xPagopaPnCxGroups );
				throw new PnInvalidInputException( ERROR_CODE_DELIVERY_INVALIDPARAMETER_GROUP, notificationGroup, logMessage );
			}
		}

	}

	private void setPagoPaIntMode(NewNotificationRequestV23 newNotificationRequest) {
		// controllo se non é stato settato il valore pagoPaIntMode dalla PA
		if ( ObjectUtils.isEmpty( newNotificationRequest.getPagoPaIntMode() ) ) {
			// verifico che nessun destinatario ha un pagamento
			if ( newNotificationRequest.getRecipients().stream()
					.noneMatch(notificationRecipient -> notificationRecipient.getPayments() != null && !notificationRecipient.getPayments().isEmpty())) {
				// metto default a NONE
				newNotificationRequest.setPagoPaIntMode(NewNotificationRequestV23.PagoPaIntModeEnum.NONE);
			} else {
				// qualche destinatario ha un pagamento
				if (newNotificationRequest.getNotificationFeePolicy().equals( DELIVERY_MODE )) {
					newNotificationRequest.setPagoPaIntMode(NewNotificationRequestV23.PagoPaIntModeEnum.SYNC);
				} else {
					newNotificationRequest.setPagoPaIntMode( NewNotificationRequestV23.PagoPaIntModeEnum.NONE );
				}
			}
		}
	}

	private NewNotificationResponse generateResponse(InternalNotification internalNotification, String iun) {
		String notificationId = Base64Utils.encodeToString(iun.getBytes(StandardCharsets.UTF_8));

		return NewNotificationResponse.builder()
				.notificationRequestId(notificationId)
				.paProtocolNumber( internalNotification.getPaProtocolNumber() )
				.idempotenceToken( internalNotification.getIdempotenceToken() )
				.build();
	}

	private String generateIun(InternalNotification internalNotification){
		Instant createdAt = clock.instant();
		String iun = iunGenerator.generatePredictedIun( createdAt );
		log.debug( "Generated iun={}", iun );
		internalNotification.iun( iun );
		internalNotification.sentAt( createdAt.atOffset( ZoneOffset.UTC ) );
		return iun;
	}

	private void doSaveWithRethrow( InternalNotification internalNotification) throws PnIdConflictException {
		log.debug( "tryMultipleTimesToHandleIunCollision: start paProtocolNumber={}",
				internalNotification.getPaProtocolNumber() );
		doSave(internalNotification);
	}


	private void doSave(InternalNotification internalNotification) throws PnIdConflictException {
		log.info("Store the notification metadata for iun={}", internalNotification.getIun());
		notificationDao.addNotification(internalNotification);
	}

}
