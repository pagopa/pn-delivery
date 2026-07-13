package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.delivery.exception.PnForbiddenException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalTimelineElementV1;
import it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy.EventBridgeNotificationViewedStrategy;
import it.pagopa.pn.delivery.models.InformalNotificationDetail;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.authorization.CxType;
import it.pagopa.pn.delivery.svc.search.InformalTimelineEnricher;
import it.pagopa.pn.delivery.svc.search.MessageEnricher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_USER_ID_NOT_RECIPIENT_OR_DELEGATOR;
import static it.pagopa.pn.delivery.utils.PgUtils.checkAuthorizationPG;

@Service
@Slf4j
public class InformalNotificationDetailRetrieverStrategy implements NotificationDetailRetrieverStrategy<InformalNotificationDetail> {

	private final Clock clock;
	private final EventBridgeNotificationViewedStrategy notificationAcknowledgementProducer;
	private final InformalTimelineEnricher informalTimelineEnricher;
	private final NotificationRetrieverService notificationRetrieverService;
	private final MessageEnricher messageEnricher;


	@Autowired
	public InformalNotificationDetailRetrieverStrategy(Clock clock,
													   EventBridgeNotificationViewedStrategy notificationAcknowledgementProducer,
                                                       InformalTimelineEnricher informalTimelineEnricher,
                                                       NotificationRetrieverService notificationRetrieverService, MessageEnricher messageEnricher) {
		this.clock = clock;
		this.notificationAcknowledgementProducer = notificationAcknowledgementProducer;
        this.informalTimelineEnricher = informalTimelineEnricher;
        this.notificationRetrieverService = notificationRetrieverService;
        this.messageEnricher = messageEnricher;
    }

	/**
	 * Get the full detail of a notification by IUN
	 *
	 * @param iun unique identifier of a Notification
	 * @param withTimeline true if return Notification with Timeline and StatusHistory
	 * @param requestBySender true if the request came from Sender
	 * @param senderId unique identifier of the sender
	 *
	 * @return Notification DTO
	 *
	 */
	public InformalNotificationDetail getNotificationInformation(String iun, boolean withTimeline, boolean withMessage, boolean requestBySender, String senderId) {
		log.debug( "Retrieve notification by iun={} withTimeline={} requestBySender={} START", iun, withTimeline, requestBySender );
		InformalNotificationDetail informalNotificationDetail =  notificationRetrieverService.loadAndEnrichNotificationDetail(iun, withTimeline, requestBySender, senderId, new InformalNotificationDetail(), informalTimelineEnricher);
		if(withMessage) {
			messageEnricher.enrichInternalNotification(informalNotificationDetail.getNotification());
		}
		return informalNotificationDetail;
	}

	/**
	 * Get the full detail of a notification by IUN with senderId check
	 *
	 * @param iun unique identifier of a Notification
	 * @param senderId unique identifier of the sender
	 * @throws PnNotificationNotFoundException if sender is not notification sender
	 *
	 * @return Notification DTO
	 *
	 */
	@Override
	public InformalNotificationDetail getNotificationInformationWithSenderIdCheck(String iun, String senderId, List<String> groups) {
		return getNotificationInformationWithSenderIdCheck(iun, senderId, groups, false);
	}

	public InformalNotificationDetail getNotificationInformationWithSenderIdCheck(String iun, String senderId, List<String> groups, boolean withMessage) {
		log.debug( "Retrieve complete notification with sender check and message by iun={} senderId={} withMessage={} START", iun, senderId, withMessage );
		InformalNotificationDetail informalNotificationDetail = notificationRetrieverService.loadCheckAndEnrichNotificationDetail(iun, senderId, groups, new InformalNotificationDetail(), informalTimelineEnricher);
		if(withMessage) {
			messageEnricher.enrichInternalNotification(informalNotificationDetail.getNotification());
		}
		return informalNotificationDetail;
	}

	@Override
	public InformalNotificationDetail getNotificationInformation(String senderId, String paProtocolNumber, String idempotenceToken, List<String> groups) {
		String iun = notificationRetrieverService.resolveIunFromRequestId(senderId, paProtocolNumber, idempotenceToken);
		return getNotificationInformationWithSenderIdCheck( iun, senderId, groups );
	}

	/**
	 * Get the full detail of a notification by IUN and notify viewed event
	 *
	 * @param iun                	unique identifier of a Notification
	 * @param internalAuthHeader	header cx-*
	 * @return Notification
	 */
	public InformalNotificationDetail getNotificationAndNotifyViewedEvent(
			String iun,
			InternalAuthHeader internalAuthHeader,
			PnAuditLogEvent logEvent,
			boolean withMessage
	) {
		log.debug("Start getInformalNotificationAndNotifyViewedEvent for {}", iun);

		InformalNotificationDetail notificationDetail = getNotificationInformation(iun, true, withMessage, false, null);
		InternalNotification notification = notificationDetail.getNotification();
		if (checkAuthorizationPG(internalAuthHeader.cxType(), internalAuthHeader.xPagopaPnCxGroups())) {
			log.error( "only a PG admin can access this resource" );
			throw new PnForbiddenException(ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
		}

		String recipientId = internalAuthHeader.xPagopaPnCxId();
		int recipientIndex = getRecipientIndexFromRecipientId(notification, recipientId);
		logEvent.getMdc().put(MDCUtils.MDC_PN_RECIPIENT_ID_KEY, recipientId);
		filterTimelinesByRecipient(notificationDetail, internalAuthHeader, recipientIndex);
		filterRecipients(notification, internalAuthHeader, recipientIndex);
		notifyNotificationViewedEvent(notification, recipientIndex, internalAuthHeader);
		return notificationDetail;
	}


	private void filterTimelinesByRecipient(InformalNotificationDetail informalNotificationDetail, InternalAuthHeader internalAuthHeader, int recipientIndex) {
		if (!CxType.PA.name().equals(internalAuthHeader.cxType())) {
			//se il servizio è invocato da un destinatario, devo filtrare la timeline solo per lo specifico destinatario (o suo delegato)
			//filtro (cyType != PA) superfluo poiché attualmente il servizio è invocato solo lato destinatario
			List<InformalTimelineElementV1> timeline = informalNotificationDetail.getTimeline();
			log.debug("Timelines size before filter: {}, for iun {} and recIndx {}", timeline.size(), informalNotificationDetail.getNotification().getIun(), recipientIndex);

			List<InformalTimelineElementV1> filteredTimelineElements = timeline.stream().filter(timelineElement -> timelineElement.getDetails() == null ||
							timelineElement.getDetails().getRecIndex() == null ||
							timelineElement.getDetails().getRecIndex() == recipientIndex)
					.toList();

			log.debug("Timelines size after filter: {}, for iun {} and recIndx {}", timeline.size(), informalNotificationDetail.getNotification().getIun(), recipientIndex);
			informalNotificationDetail.setTimeline(filteredTimelineElements);
		}
	}

	private void filterRecipients(InternalNotification internalNotification, InternalAuthHeader internalAuthHeader, int recipientIndex) {
		if (!CxType.PA.name().equals(internalAuthHeader.cxType())) {
			//se il servizio è invocato da un destinatario (o suo delegato), devo vedere tutti i dati in chiaro solo per lo specifico destinatario
			//filtro (cyType != PA) superfluo poiché attualmente il servizio è invocato solo lato destinatario

			//"pulisco gli altri destinatari"
			var filteredNotificationRecipients = new ArrayList<NotificationRecipient>();
			for(int i = 0; i< internalNotification.getRecipients().size(); i ++) {
				NotificationRecipient recipient = internalNotification.getRecipients().get(i);
				if(i != recipientIndex) {
					recipient = NotificationRecipient.builder()
							.recipientType(recipient.getRecipientType())
							.internalId(recipient.getInternalId())
							.build();
				}
				filteredNotificationRecipients.add(recipient);
			}

			internalNotification.setRecipients(filteredNotificationRecipients);
		}
	}

	private void notifyNotificationViewedEvent(InternalNotification notification, int recipientIndex, InternalAuthHeader internalAuthHeader) {
		String iun = notification.getIun();
		log.info("Send \"notification acknowledgement\" event for iun={} and recipientIndex={}", iun, recipientIndex);
		Instant createdAt = clock.instant();
		notificationAcknowledgementProducer.sendNotificationViewed( iun, createdAt, recipientIndex, null, internalAuthHeader.xPagopaPnSrcCh(), internalAuthHeader.xPagopaPnSrcChDetails() );
	}

	private int getRecipientIndexFromRecipientId(InternalNotification internalNotification, String recipientId) {
		int recIndex = IntStream.range(0, internalNotification.getRecipientIds().size())
				.filter(i -> internalNotification.getRecipientIds().get(i).equals(recipientId))
				.findFirst().orElse(-1);

		if( recIndex == -1 ) {
			log.debug("Recipient not found for iun={} and recipientId={} ", internalNotification.getIun(), recipientId );
			throw new PnNotFoundException("Notification not found" ,"Notification with iun=" +
					internalNotification.getIun() + " do not have recipient/delegator=" + recipientId,
					ERROR_CODE_DELIVERY_USER_ID_NOT_RECIPIENT_OR_DELEGATOR );
		}

		return recIndex;
	}
}
