package it.pagopa.pn.delivery;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.UUID;

import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeliveryService {

	private final Clock clock;
	private final NotificationDao notificationDao;
	private final NewNotificationProducer newNotificationEventProducer;

	@Autowired
	public DeliveryService(Clock clock, NotificationDao notificationDao, NewNotificationProducer newNotificationEventProducer) {
		this.clock = clock;
		this.notificationDao = notificationDao;
		this.newNotificationEventProducer = newNotificationEventProducer;
	}

	public NewNotificationResponse receiveNotification(Notification notification) {
		checkNewNotificationBeforeInsert( notification );

		// - save
		int K = 3;

		String iun = doSaveNewNotification( notification, K);


		// - push event to workflow engine

		return NewNotificationResponse.builder()
				.iun( iun )
				.paNotificationId( notification.getPaNotificationId() )
				.build();
	}

	private String doSaveNewNotification( Notification notification, int K) {
		String iun;
		boolean duplicatedIun;
		int iunConflictCounter = 0;

		do {
			iun = generateIun();

			String paId = notification.getSender().getPaId();
			NewNotificationEvent event = buildNewNotificationEvent( iun, paId );
			Notification toInsert = notification.toBuilder()
					.iun( iun )
					.sender( NotificationSender.builder()
							.paId( paId )
							.build()
					)
					.build();

			newNotificationEventProducer.push( event );
			duplicatedIun = this.tryInsertOnce( toInsert );

			if( duplicatedIun ) {
				iunConflictCounter += 1;
			}
		}
		while ( duplicatedIun && iunConflictCounter < K);

		return iun;
	}

	private NewNotificationEvent buildNewNotificationEvent(String iun, String paId ) {
		GenericEvent<StandardEventHeader, NewNotificationEvent.Payload> genericEvent = GenericEvent.<StandardEventHeader, NewNotificationEvent.Payload>builder()
				.header( StandardEventHeader.builder()
						.iun( iun )
						.eventId( iun + "_start" )
						.createdAt( clock.instant() )
						.eventType( EventType.NEW_NOTIFICATION )
						.publisher( EventPublisher.DELIVERY.name() )
						.build()
				)
				.payload( NewNotificationEvent.Payload.builder()
						.paId( paId )
						.build()
				)
				.build();
		return new NewNotificationEvent( genericEvent );
	}

	private boolean tryInsertOnce( Notification notification) {
		boolean duplicatedIun = false;
		try {
			notificationDao.addNotification( notification );
		}
		catch (IdConflictException exc) {
			duplicatedIun = true;
		}
		return duplicatedIun;
	}

	private void checkNewNotificationBeforeInsert( Notification notification) {
		if (!checkPaNotificationId( notification.getSender().getPaId() )) {
			throw new IllegalArgumentException("Invalid paID"); // FIXME gestione messaggistica
		}

		String paNotificationId = notification.getPaNotificationId();
		if (!checkPaNotificationId(paNotificationId)) {
			throw new IllegalArgumentException("Invalid paNotificationId"); // FIXME gestione messaggistica
		}

		List<NotificationRecipient> recipients = notification.getRecipients();
		if (!checkRecipients(recipients)) {
			throw new IllegalArgumentException("Invalid recipients"); // FIXME gestione messaggistica
		}
	}

	private String generateIun() {
		String uuid = UUID.randomUUID().toString();
		Instant now = Instant.now(clock);
		OffsetDateTime nowUtc = now.atOffset( ZoneOffset.UTC );
		int year = nowUtc.get( ChronoField.YEAR_OF_ERA);
		int month = nowUtc.get( ChronoField.MONTH_OF_YEAR);
		return year + month + '-' + uuid;
	}

	private boolean checkPaNotificationId(String paNotificationId) {
		return StringUtils.isNotBlank(paNotificationId);
	}

	private boolean checkRecipients(List<NotificationRecipient> recipients) {
		return (recipients != null && !recipients.isEmpty() 
					&& checkRecipientsItems(recipients));
	}

	private boolean checkRecipientsItems(List<NotificationRecipient> recipients) {
		for (NotificationRecipient recipient : recipients) {
			if (recipient == null || StringUtils.isBlank(recipient.getFc()) 
					|| (recipient.getPhysicalAddress() == null )) {
				return false;
			}
		}
		
		return true;
	}

}
