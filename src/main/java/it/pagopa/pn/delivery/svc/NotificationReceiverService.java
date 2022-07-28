package it.pagopa.pn.delivery.svc;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;


import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

@Service
@Slf4j
public class NotificationReceiverService {

	private final Clock clock;
	private final NotificationDao notificationDao;
	private final NewNotificationProducer newNotificationEventProducer;
	private final NotificationReceiverValidator validator;
	private final ModelMapperFactory modelMapperFactory;

	private final IunGenerator iunGenerator = new IunGenerator();

	@Autowired
	public NotificationReceiverService(
			Clock clock,
			NotificationDao notificationDao,
			NewNotificationProducer newNotificationEventProducer,
			NotificationReceiverValidator validator,
			ModelMapperFactory modelMapperFactory) {
		this.clock = clock;
		this.notificationDao = notificationDao;
		this.newNotificationEventProducer = newNotificationEventProducer;
		this.validator = validator;
		this.modelMapperFactory = modelMapperFactory;
	}

	/**
	 * Store metadata and documents about a new notification request
	 *
	 * @param xPagopaPnCxId Public Administration id
	 * @param newNotificationRequest Public Administration notification request that PN have to forward to
	 *                     one or more recipient
	 * @return A model with the generated IUN and the paNotificationId sent by the
	 *         Public Administration
	 */
	public NewNotificationResponse receiveNotification(String xPagopaPnCxId, NewNotificationRequest newNotificationRequest) throws IdConflictException {
		log.info("New notification storing START");
		log.debug("New notification storing START for={}", newNotificationRequest);
		validator.checkNewNotificationRequestBeforeInsertAndThrow(newNotificationRequest);
		log.debug("Validation OK for paProtocolNumber={}", newNotificationRequest.getPaProtocolNumber() );

		ModelMapper modelMapper = modelMapperFactory.createModelMapper( NewNotificationRequest.class, InternalNotification.class );
		InternalNotification internalNotification = modelMapper.map(newNotificationRequest, InternalNotification.class);

		internalNotification.setSenderPaId( xPagopaPnCxId );

		String iun = doSaveWithRethrow(internalNotification);

		NewNotificationResponse response = generateResponse(internalNotification, iun);

		log.info("New notification storing END {}", response);
		return response;
	}

	private NewNotificationResponse generateResponse(InternalNotification internalNotification, String iun) {
		String notificationId = Base64Utils.encodeToString(iun.getBytes(StandardCharsets.UTF_8));
		
		return NewNotificationResponse.builder()
				.notificationRequestId(notificationId)
				.paProtocolNumber( internalNotification.getPaProtocolNumber() )
				.idempotenceToken( internalNotification.getIdempotenceToken() )
				.build();
	}

	private String doSaveWithRethrow( InternalNotification internalNotification) throws IdConflictException {
		log.debug( "tryMultipleTimesToHandleIunCollision: start paProtocolNumber={}",
				internalNotification.getPaProtocolNumber() );

		Instant createdAt = clock.instant();
		String iun = iunGenerator.generatePredictedIun( createdAt );
		log.debug( "Generated iun={}", iun );
		doSave(internalNotification, createdAt, iun);
		return iun;
	}
	

	private void doSave(InternalNotification internalNotification, Instant createdAt, String iun) throws IdConflictException {
		String paId = internalNotification.getSenderPaId();

		log.debug("Generate tokens for iun={}", iun);
		// generazione token per ogni destinatario
		Map<NotificationRecipient,String> tokens = generateToken( internalNotification.getRecipients(), iun );

		internalNotification.iun( iun );
		internalNotification.sentAt( createdAt.atOffset( ZoneOffset.UTC ) );
		internalNotification.setTokens( tokens );


		log.info("Store the notification metadata for iun={}", iun);
		notificationDao.addNotification(internalNotification, () -> {
			// - Will be delayed from the receiver
			log.debug("Send \"new notification\" event for iun={}", iun);
			newNotificationEventProducer.sendNewNotificationEvent( paId, iun, createdAt);
		});
	}


	private Map<NotificationRecipient,String> generateToken(List<NotificationRecipient> recipientList, String iun) {
		Map<NotificationRecipient,String> tokens = new HashMap<>();
		for (NotificationRecipient recipient : recipientList) {
			tokens.put(recipient, generateToken(iun, recipient.getTaxId()));
		}
		return tokens;
	}

	public String generateToken(String iun, String taxId) {
		return iun + "_" + taxId + "_" + UUID.randomUUID();
	}


}
