package it.pagopa.pn.delivery;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import it.pagopa.pn.commons.kafka.KafkaConfigs;
import it.pagopa.pn.delivery.model.message.Message;
import it.pagopa.pn.delivery.model.message.Message.Type;
import it.pagopa.pn.delivery.model.notification.Notification;
import it.pagopa.pn.delivery.model.notification.NotificationRecipient;
import it.pagopa.pn.delivery.repository.NotificationRepository;

@Service
public class DeliveryService {

	private final Clock clock;
	private final NotificationRepository notificationRepository;
	private final KafkaTemplate<String, Message> kafkaTemplate;
	private final KafkaConfigs kafkaConfigs;

	@Autowired
	public DeliveryService(Clock clock, NotificationRepository notificationRepository,
			KafkaTemplate<String, Message> kafkaTemplate, KafkaConfigs kafkaConfigs) {
		this.clock = clock;
		this.notificationRepository = notificationRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.kafkaConfigs = kafkaConfigs;
	}

	public Notification receiveNotification(String paId, Notification notification) {

		if (!checkPaNotificationId(paId)) {
			throw new IllegalArgumentException("Invalid paID"); // FIXME gestione messaggistica
		}
		notification.getSender().setPaId(paId.trim());

		String paNotificationId = notification.getPaNotificationId();
		if (!checkPaNotificationId(paNotificationId)) {
			throw new IllegalArgumentException("Invalid paNotificationId"); // FIXME gestione messaggistica
		}

		List<NotificationRecipient> recipients = notification.getRecipients();
		if (!checkRecipients(recipients)) {
			throw new IllegalArgumentException("Invalid recipients"); // FIXME gestione messaggistica
		}

		// - Verificare sha256

		String iun = generateIun();
		notification.setIun(iun);

		// - save
		Notification addedNotification = notificationRepository.save(notification);
		
		// - push message to kafka
		Message message = new Message();
		message.setIun(iun);
		message.setSentDate(Instant.now());
		message.setMessageType(Type.TYPE1);
		kafkaTemplate.send(kafkaConfigs.getTopic(), message);
		
		return addedNotification;
	}

	private String generateIun() {
		String uuid = UUID.randomUUID().toString();
		Instant now = Instant.now(clock);
		OffsetDateTime nowUtc = now.atOffset(ZoneOffset.UTC);
		
		return nowUtc.get(ChronoField.YEAR_OF_ERA) + nowUtc.get(ChronoField.MONTH_OF_YEAR) + '-' + uuid;
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
					|| (recipient.getPhysicalAddress() == null 
						|| recipient.getPhysicalAddress().isEmpty())) {
				return false;
			}
		}
		
		return true;
	}

}
