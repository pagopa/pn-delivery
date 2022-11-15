package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class NotificationReceiverService {

	private final Clock clock;
	private final NotificationDao notificationDao;
	private final NotificationReceiverValidator validator;
	private final ModelMapperFactory modelMapperFactory;

	private final IunGenerator iunGenerator = new IunGenerator();

	@Autowired
	public NotificationReceiverService(
			Clock clock,
			NotificationDao notificationDao,
			NotificationReceiverValidator validator,
			ModelMapperFactory modelMapperFactory) {
		this.clock = clock;
		this.notificationDao = notificationDao;
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
	public NewNotificationResponse receiveNotification(String xPagopaPnCxId, NewNotificationRequest newNotificationRequest) throws PnIdConflictException {
		log.info("New notification storing START");
		log.debug("New notification storing START paProtocolNumber={} idempotenceToken={}",
				newNotificationRequest.getPaProtocolNumber(), newNotificationRequest.getIdempotenceToken());
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

	private String doSaveWithRethrow( InternalNotification internalNotification) throws PnIdConflictException {
		log.debug( "tryMultipleTimesToHandleIunCollision: start paProtocolNumber={}",
				internalNotification.getPaProtocolNumber() );

		Instant createdAt = clock.instant();
		String iun = iunGenerator.generatePredictedIun( createdAt );
		log.debug( "Generated iun={}", iun );
		doSave(internalNotification, createdAt, iun);
		return iun;
	}
	

	private void doSave(InternalNotification internalNotification, Instant createdAt, String iun) throws PnIdConflictException {

		internalNotification.iun( iun );
		internalNotification.sentAt( createdAt.atOffset( ZoneOffset.UTC ) );
		
		log.info("Store the notification metadata for iun={}", iun);
		notificationDao.addNotification(internalNotification);
	}

}
