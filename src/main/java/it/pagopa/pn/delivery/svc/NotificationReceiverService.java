package it.pagopa.pn.delivery.svc;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.DirectAccessTokenDao;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.utils.EncodingUtils;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationReceiverService {

	private final Clock clock;
	private final NotificationDao notificationDao;
	private final DirectAccessTokenDao directAccessTokenDao;
	private final NewNotificationProducer newNotificationEventProducer;
	private final AttachmentService attachmentSaver;
	private final NotificationReceiverValidator validator;

	@Autowired
	public NotificationReceiverService(
			Clock clock,
			NotificationDao notificationDao,
			DirectAccessTokenDao directAccessTokenDao,
			NewNotificationProducer newNotificationEventProducer,
			AttachmentService attachmentSaver,
			NotificationReceiverValidator validator
	) {
		this.clock = clock;
		this.notificationDao = notificationDao;
		this.directAccessTokenDao = directAccessTokenDao;
		this.newNotificationEventProducer = newNotificationEventProducer;
		this.attachmentSaver = attachmentSaver;
		this.validator = validator;
	}

	/**
	 * Store metadata and documents about a new notification request
	 *
	 * @param notification Public Administration notification request that PN have to forward to
	 *                     one or more recipient
	 * @return A model with the generated IUN and the paNotificationId sent by the
	 *         Public Administration
	 */
	public NewNotificationResponse receiveNotification(Notification notification) {
		log.info("New notification storing START");
		log.debug("New notification storing START for={}", notification );
		validator.checkNewNotificationBeforeInsertAndThrow( notification );
		log.debug("Validation OK for paNotificationId={}", notification.getPaNotificationId() );

		String iun = doSaveWithRethrow( notification );

		NewNotificationResponse response = generateResponse(notification, iun);

		log.info("New notification storing END {}", response);
		return response;
	}

	private NewNotificationResponse generateResponse(Notification notification, String iun) {
		String notificationId = EncodingUtils.base64Encoding(iun);
		
		return NewNotificationResponse.builder()
				.notificationId( notificationId )
				.paNotificationId( notification.getPaNotificationId() )
				.build();
	}

	private String doSaveWithRethrow( Notification notification ) {
		String iun = generatePredictedIun( notification );
		
		log.debug( "tryMultipleTimesToHandleIunCollision: start iun={} notificationId={} paNotificationId={}",
				iun, notification.getPaNotificationId() );

		try {
			doSave(notification, iun);
		}
		catch ( IdConflictException exc ) {
			log.error("Duplicated iun={}", iun );
			throw new PnInternalException( "Duplicated iun=" + iun, exc );
		}

		return iun;
	}
	

	private void doSave( Notification notification, String iun) throws IdConflictException {
		Instant createdAt = clock.instant();
		String paId = notification.getSender().getPaId();

		log.debug("Generate tokens for iun={}", iun);
		// generazione token per ogni destinatario
		List<NotificationRecipient> recipientsWithToken = addDirectAccessTokenToRecipients(notification, iun);

		Notification notificationWithIun = notification.toBuilder()
				.iun( iun )
				.sentAt( createdAt )
				.sender( NotificationSender.builder()
						.paId( paId )
						.build()
				)
				.recipients( recipientsWithToken )
				.build();

		log.info("Start Attachment save for iun={}", iun);
		Notification notificationWithCompleteMetadata = attachmentSaver.saveAttachments( notificationWithIun );

		// - Will be delayed from the receiver
		log.debug("Send \"new notification\" event for iun={}", iun);
		newNotificationEventProducer.sendNewNotificationEvent( paId, iun, createdAt);

		log.info("Store the notification metadata for iun={}", iun);
		notificationDao.addNotification( notificationWithCompleteMetadata );
	}
	
	private List<NotificationRecipient> addDirectAccessTokenToRecipients(Notification notification, String iun) throws IdConflictException {
		List<NotificationRecipient> recipients = notification.getRecipients();
		List<NotificationRecipient> recipientsWithToken = new ArrayList<>(recipients.size());
		for (NotificationRecipient recipient : recipients) {
			String token = generateToken( );
			// chiamata al dao per inserimento tokens
			directAccessTokenDao.addDirectAccessToken(DirectAccessToken.builder()
					.token( token )
					.iun(iun)
					.taxId( recipient.getTaxId() )
					.build());

			recipientsWithToken.add( recipient.toBuilder().token( token ).build() );
		}
		return recipientsWithToken;
	}

	private String generatePredictedIun(Notification notification) {
		NotificationSender sender = notification.getSender();
		String paId = sender.getPaId();
		String paNotificationId = notification.getPaNotificationId();
		String sqsSafePaNotificationId = paNotificationId.replaceAll( "[^a-zA-Z0-9-_]", "-" );
		return String.format("%s-%s", paId, sqsSafePaNotificationId);
	}


	private String generateToken() {
		return UUID.randomUUID().toString();
	}

}
