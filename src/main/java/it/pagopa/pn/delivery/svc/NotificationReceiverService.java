package it.pagopa.pn.delivery.svc;

import java.time.Clock;
import java.time.Instant;
import java.util.*;


import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.utils.EncodingUtils;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationReceiverService {

	private final Clock clock;
	private final NotificationDao notificationDao;
	private final DirectAccessService directAccessService;
	private final NewNotificationProducer newNotificationEventProducer;
	private final AttachmentService attachmentSaver;
	private final NotificationReceiverValidator validator;

	private final IunGenerator iunGenerator = new IunGenerator();

	@Autowired
	public NotificationReceiverService(
			Clock clock,
			NotificationDao notificationDao,
			DirectAccessService directAccessService,
			NewNotificationProducer newNotificationEventProducer,
			AttachmentService attachmentSaver,
			NotificationReceiverValidator validator
	) {
		this.clock = clock;
		this.notificationDao = notificationDao;
		this.directAccessService = directAccessService;
		this.newNotificationEventProducer = newNotificationEventProducer;
		this.attachmentSaver = attachmentSaver;
		this.validator = validator;
	}

	/**
	 * Store metadata and documents about a new notification request
	 *
	 * @param internalNotification Public Administration notification request that PN have to forward to
	 *                     one or more recipient
	 * @return A model with the generated IUN and the paNotificationId sent by the
	 *         Public Administration
	 */
	public NewNotificationResponse receiveNotification(InternalNotification internalNotification) {
		log.info("New notification storing START");
		log.debug("New notification storing START for={}", internalNotification);
		validator.checkNewNotificationBeforeInsertAndThrow(internalNotification);
		log.debug("Validation OK for paProtocolNumber={}", internalNotification.getPaProtocolNumber() );

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
		//List<NotificationRecipient> recipientsWithToken = addDirectAccessTokenToRecipients(notification, iun);
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
	
	/*private List<NotificationRecipient> addDirectAccessTokenToRecipients(Notification notification, String iun) {
		List<NotificationRecipient> recipients = notification.getRecipients();
		List<NotificationRecipient> recipientsWithToken = new ArrayList<>(recipients.size());
		for (NotificationRecipient recipient : recipients) {
			String token = directAccessService.generateToken(iun, recipient.getTaxId());
			recipientsWithToken.add( recipient.toBuilder().token( token ).build() );
		}
		return recipientsWithToken;
	}*/

	private Map<NotificationRecipient,String> generateToken(List<NotificationRecipient> recipientList, String iun) {
		Map<NotificationRecipient,String> tokens = new HashMap<>();
		for (NotificationRecipient recipient : recipientList) {
			tokens.put(recipient, directAccessService.generateToken(iun, recipient.getTaxId()));
		}
		return tokens;
	}


}
