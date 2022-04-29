package it.pagopa.pn.delivery.svc;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.utils.EncodingUtils;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationReceiverService {

	private static final char[] IUN_CHARS = new char[] {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
	private static final String SEPARATOR = "-";

	private final Clock clock;
	private final NotificationDao notificationDao;
	private final DirectAccessService directAccessService;
	private final NewNotificationProducer newNotificationEventProducer;
	private final AttachmentService attachmentSaver;
	private final NotificationReceiverValidator validator;

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
		String iun = generatePredictedIun( notification.getSentAt().toString() );
		
		log.debug( "tryMultipleTimesToHandleIunCollision: start iun={} paNotificationId={}",
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
	
	private List<NotificationRecipient> addDirectAccessTokenToRecipients(Notification notification, String iun) {
		List<NotificationRecipient> recipients = notification.getRecipients();
		List<NotificationRecipient> recipientsWithToken = new ArrayList<>(recipients.size());
		for (NotificationRecipient recipient : recipients) {
			String token = directAccessService.generateToken(iun, recipient.getTaxId());
			recipientsWithToken.add( recipient.toBuilder().token( token ).build() );
		}
		return recipientsWithToken;
	}

	private String generatePredictedIun(String creationDate) {
		String[] creationDateSplit = creationDate.split( SEPARATOR );
		String randStringPart = generateRandomString(4, 3, '-');
		String monthPart = creationDateSplit[0] + creationDateSplit[1];
		char controlChar = generateControlChar( randStringPart, monthPart );
		return randStringPart + SEPARATOR + monthPart + SEPARATOR + controlChar + SEPARATOR + "1";
	}

	private char generateControlChar(String randStringPart, String monthPart) {
		int sum=0;
		for (int i = 0; i < randStringPart.length(); i++) {
			char singleChar = randStringPart.charAt( i );
			sum += new String(IUN_CHARS).indexOf( singleChar ) + 1;
		}
		for (int i = 0; i < monthPart.length(); i++) {
			char singleChar = monthPart.charAt( i );
			sum += Integer.parseInt(String.valueOf(singleChar));
		}
		int mod = (sum % IUN_CHARS.length);
		return IUN_CHARS[mod];
	}

	private String generateRandomString(int segmentLength, int segmentQuantity, char separator) {
		Random random = new Random();
		StringBuilder buffer = new StringBuilder((segmentLength + 1) * segmentQuantity);
		for (int s = 0 ; s < segmentQuantity; s++) {
			if (s > 0) {
				buffer.append(separator);
			}
			for (int i = 0; i < segmentLength; i++) {
				int randomLimitedInt = random.nextInt(IUN_CHARS.length);
				buffer.append(IUN_CHARS[randomLimitedInt]);
			}
		}
		return buffer.toString();
	}
}
