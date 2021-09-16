package it.pagopa.pn.delivery.svc.notificationacknoledgement;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationAcknowledgementProducer;
import it.pagopa.pn.delivery.svc.recivenotification.SaveAttachmentService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationAcknoledgementService {

	private final SaveAttachmentService attachmentService;
	private final Clock clock;
	private final NotificationAcknowledgementProducer notificationAcknowledgementProducer;
	private final NotificationDao notificationDao;

	@Autowired
	public NotificationAcknoledgementService( Clock clock,
			SaveAttachmentService attachmentService,
			NotificationAcknowledgementProducer notificationAcknowledgementProducer, NotificationDao notificationDao ) {
		this.clock = clock;
		this.attachmentService = attachmentService;
		this.notificationAcknowledgementProducer = notificationAcknowledgementProducer;
		this.notificationDao = notificationDao;
	}

	/**
	 * Download a document from the S3 storage
	 *
	 * @param iun unique identifier of a Notification
	 * @param documentIndex index of the documents array
	 * 
	 * @return HTTP response containing the status code, the headers, and the body
 	 *	of the document to download
	 * 
	 */
	public ResponseEntity<Resource> notificationAcknowledgement(String iun, int documentIndex, String userId ) {
		log.debug( "Document download START for iun and documentIndex {}" , iun, documentIndex);
		
		log.info( "Received ACTION iun={} eventId={} actionType={}", iun );
        Optional<Notification> notification = notificationDao.getNotificationByIun( iun );
        if( !notification.isPresent() ) {
            log.debug("Notification not found for iun {}", iun );
			throw new PnInternalException( "Notification not found for iun " + iun );
        }
                
        int recepientIndex = IntStream.range( 0, notification.get().getRecipients().size() )
                						.filter( i -> userId.equals( notification.get().getRecipients().get( i ).getTaxId() ) )
                						.findFirst()
                						.orElse( -1 );
        
        if( recepientIndex == -1 ) {
            log.debug("Recipient not found for iun and userId{} ", iun, userId );
			throw new PnInternalException( "Notification not found for iun " + iun + " and userId " + userId );
        }
		 			
		ResponseEntity<Resource> response = attachmentService.loadDocument( iun, documentIndex );
		
		log.debug("Send \"notification acknowlwdgement\" event for iun {}", iun);
		Instant createdAt = clock.instant();
		notificationAcknowledgementProducer.sendNotificationAcknowlwdgement( iun, createdAt, recepientIndex );
	
		log.debug("downloadDocument: response {}", response);
	    return response;
	}

}
