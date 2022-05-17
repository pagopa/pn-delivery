package it.pagopa.pn.delivery.svc;

import java.time.Clock;
import java.time.Instant;
import java.util.*;


import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.utils.EncodingUtils;
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

@Service
@Slf4j
public class NotificationReceiverService {

	private final Clock clock;
	private final NotificationDao notificationDao;
	private final NewNotificationProducer newNotificationEventProducer;
	private final AttachmentService attachmentSaver;
	private final NotificationReceiverValidator validator;
	private final ModelMapperFactory modelMapperFactory;

	private final IunGenerator iunGenerator = new IunGenerator();

	@Autowired
	public NotificationReceiverService(
			Clock clock,
			NotificationDao notificationDao,
			NewNotificationProducer newNotificationEventProducer,
			AttachmentService attachmentSaver,
			NotificationReceiverValidator validator,
			ModelMapperFactory modelMapperFactory) {
		this.clock = clock;
		this.notificationDao = notificationDao;
		this.newNotificationEventProducer = newNotificationEventProducer;
		this.attachmentSaver = attachmentSaver;
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
	public NewNotificationResponse receiveNotification(String xPagopaPnCxId, NewNotificationRequest newNotificationRequest) {
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
		String notificationId = EncodingUtils.base64Encoding(iun);
		
		return NewNotificationResponse.builder()
				.notificationRequestId(notificationId)
				.paProtocolNumber( internalNotification.getPaProtocolNumber() )
				.build();
	}

	private String doSaveWithRethrow( InternalNotification internalNotification) {
		log.debug( "tryMultipleTimesToHandleIunCollision: start paProtocolNumber={}",
				internalNotification.getPaProtocolNumber() );

		String iun = null;
		try {
			Instant createdAt = clock.instant();
			iun = iunGenerator.generatePredictedIun( createdAt );
			doSave(internalNotification, createdAt, iun);
		}
		catch ( IdConflictException exc ) {
			log.error("Duplicated iun={}", iun );
			throw new PnInternalException( "Duplicated iun=" + iun, exc );
		}

		return iun;
	}
	

	private void doSave(InternalNotification internalNotification, Instant createdAt, String iun) throws IdConflictException {
		String paId = internalNotification.getSenderPaId();

		log.debug("Generate tokens for iun={}", iun);
		// generazione token per ogni destinatario
		Map<NotificationRecipient,String> tokens = generateToken( internalNotification.getRecipients(), iun );

		internalNotification.iun( iun );
		internalNotification.sentAt( Date.from(createdAt) );
		internalNotification.setTokens( tokens );


		log.info("Start Attachment save for iun={}", iun);
		InternalNotification internalNotificationWithCompleteMetadata = attachmentSaver.saveAttachments(internalNotification);

		// - Will be delayed from the receiver
		log.debug("Send \"new notification\" event for iun={}", iun);
		newNotificationEventProducer.sendNewNotificationEvent( paId, iun, createdAt);

		log.info("Store the notification metadata for iun={}", iun);
		notificationDao.addNotification(internalNotificationWithCompleteMetadata);
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
