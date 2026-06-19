package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.delivery.exception.PnForbiddenException;
import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV28;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.LegalNotificationDetail;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.svc.authorization.CxType;
import it.pagopa.pn.delivery.svc.search.LegalTimelineEnricher;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_USER_ID_NOT_RECIPIENT_OR_DELEGATOR;
import static it.pagopa.pn.delivery.utils.NotificationUtils.isNotificationCancelled;
import static it.pagopa.pn.delivery.utils.PgUtils.checkAuthorizationPG;

@Service
@Slf4j
public class LegalNotificationDetailRetrieverStrategy implements NotificationDetailRetrieverStrategy<LegalNotificationDetail> {

	private final Clock clock;
	private final NotificationViewedProducer notificationAcknowledgementProducer;
	private final PnMandateClientImpl pnMandateClient;
	private final PnExternalRegistriesClientImpl pnExternalRegistriesClient;
	private final LegalTimelineEnricher legalTimelineEnricher;
	private final NotificationRetrieverService notificationRetrieverService;

	@Autowired
	public LegalNotificationDetailRetrieverStrategy(Clock clock,
                                                    NotificationViewedProducer notificationAcknowledgementProducer,
                                                    PnMandateClientImpl pnMandateClient,
                                                    PnExternalRegistriesClientImpl pnExternalRegistriesClient,
                                                    LegalTimelineEnricher legalTimelineEnricher,
													NotificationRetrieverService notificationRetrieverService) {
		this.clock = clock;
		this.notificationAcknowledgementProducer = notificationAcknowledgementProducer;
		this.pnMandateClient = pnMandateClient;
		this.pnExternalRegistriesClient = pnExternalRegistriesClient;
        this.legalTimelineEnricher = legalTimelineEnricher;
        this.notificationRetrieverService = notificationRetrieverService;
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
	@Override
	public LegalNotificationDetail getNotificationInformation(String iun, boolean withTimeline, boolean requestBySender, String senderId) {
		log.debug( "Retrieve notification by iun={} withTimeline={} requestBySender={} START", iun, withTimeline, requestBySender );
		return notificationRetrieverService.loadAndEnrichNotificationDetail(iun, withTimeline, requestBySender, senderId, new LegalNotificationDetail(), legalTimelineEnricher);
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
	public LegalNotificationDetail getNotificationInformationWithSenderIdCheck(String iun, String senderId, List<String> groups) {
		log.debug( "Retrieve complete notification with sender check by iun={} senderId={} START", iun, senderId );
		return notificationRetrieverService.loadCheckAndEnrichNotificationDetail(iun, senderId, groups, new LegalNotificationDetail(), legalTimelineEnricher);
	}



	@Override
	public LegalNotificationDetail getNotificationInformation(String senderId, String paProtocolNumber, String idempotenceToken, List<String> groups) {
		String iun = notificationRetrieverService.resolveIunFromRequestId(senderId, paProtocolNumber, idempotenceToken);
		return getNotificationInformationWithSenderIdCheck( iun, senderId, groups );
	}

	/**
	 * Get the full detail of a notification by IUN and notify viewed event
	 *
	 * @param iun                	unique identifier of a Notification
	 * @param internalAuthHeader	header cx-*
	 * @param mandateId 	 		id delega (opzionale)
	 * @return Notification
	 */
	@Override
	public LegalNotificationDetail getNotificationAndNotifyViewedEvent(
			String iun,
			InternalAuthHeader internalAuthHeader,
			String mandateId,
			PnAuditLogEvent logEvent
	) {
		log.debug("Start getNotificationAndSetViewed for {}", iun);

		String delegatorId = null;
		NotificationViewDelegateInfo delegateInfo = null;
		// cerco prima la notifica in DB, poi controllo la delega, visto che mi serve il paId
		// Il caso più comune infatti è che l'utente abbia il permesso di vedere una certa notifica
		LegalNotificationDetail notificationDetail = getNotificationInformation(iun, true, false, null);
		InternalNotification notification = notificationDetail.getNotification();
		if ( StringUtils.hasText( mandateId ) ) {
			InternalMandateDto mandateDto = checkMandateForNotificationDetail(
					internalAuthHeader.xPagopaPnCxId(),
					mandateId,
					notification.getSenderPaId(),
					iun,
					internalAuthHeader.cxType(),
					internalAuthHeader.xPagopaPnCxGroups(),
					notification.getSentAt()
			);
			String workflowType = mandateDto.getWorkflowType() != null ? mandateDto.getWorkflowType().getValue() : "STANDARD";
			logEvent.getMdc().put(MDCUtils.MDC_PN_MANDATE_WORKFLOW_TYPE_KEY, workflowType);
			logEvent.getMdc().put(MDCUtils.MDC_PN_DELEGATE_ID_KEY, mandateDto.getDelegate());
			delegatorId = mandateDto.getDelegator();
			delegateInfo = NotificationViewDelegateInfo.builder()
					.mandateId( mandateId )
					.internalId(internalAuthHeader.xPagopaPnCxId())
					.operatorUuid(internalAuthHeader.xPagopaPnUid())
					.delegateType( NotificationViewDelegateInfo.DelegateType.valueOf(internalAuthHeader.cxType()) )
					.build();
		} else if (checkAuthorizationPG(internalAuthHeader.cxType(), internalAuthHeader.xPagopaPnCxGroups())) {
			log.error( "only a PG admin can access this resource" );
			throw new PnForbiddenException(ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND);
		}

		String recipientId = delegatorId != null ? delegatorId : internalAuthHeader.xPagopaPnCxId();
		int recipientIndex = getRecipientIndexFromRecipientId(notification, recipientId);
		logEvent.getMdc().put(MDCUtils.MDC_PN_RECIPIENT_ID_KEY, recipientId);
		filterTimelinesByRecipient(notificationDetail, internalAuthHeader, recipientIndex);
		NotificationRetrieverService.filterRecipients(notification, internalAuthHeader, recipientIndex);
		notifyNotificationViewedEvent(notification, recipientIndex, delegateInfo, internalAuthHeader);
		return notificationDetail;
	}

	private void filterTimelinesByRecipient(LegalNotificationDetail legalNotificationDetail, InternalAuthHeader internalAuthHeader, int recipientIndex) {
		if (!CxType.PA.name().equals(internalAuthHeader.cxType())) {
			//se il servizio è invocato da un destinatario, devo filtrare la timeline solo per lo specifico destinatario (o suo delegato)
			//filtro (cyType != PA) superfluo poiché attualmente il servizio è invocato solo lato destinatario
			List<TimelineElementV28> timeline = legalNotificationDetail.getTimeline();
			log.debug("Timelines size before filter: {}", timeline.size());

			List<TimelineElementV28> filteredTimelineElements = timeline.stream().filter(timelineElement -> timelineElement.getDetails() == null ||
							timelineElement.getDetails().getRecIndex() == null ||
							timelineElement.getDetails().getRecIndex() == recipientIndex)
					.toList();

			log.debug("Timelines size after filter: {}", filteredTimelineElements.size());
			legalNotificationDetail.setTimeline(filteredTimelineElements);
		}
	}

	private @NotNull InternalMandateDto checkMandateForNotificationDetail(String userId, String mandateId, String paId, String iun, String recipientType, List<String> cxGroups, OffsetDateTime notificationSentAt) {
		CxTypeAuthFleet cxTypeAuthFleet = StringUtils.hasText(recipientType) ? CxTypeAuthFleet.valueOf(recipientType) : null;
		String rootSenderId = pnExternalRegistriesClient.getRootSenderId(paId);
		List<InternalMandateDto> mandates = pnMandateClient.listMandatesByDelegateV2(userId, mandateId, cxTypeAuthFleet, cxGroups, notificationSentAt, iun, rootSenderId);

		if(!mandates.isEmpty()) {
			// Considerando che abbiamo fornito il filtro sul mandateId, dovrebbe esserci al massimo una delega valida.
			InternalMandateDto mandate = mandates.get(0);
			log.info( "Valid mandate for notification detail for delegate={}", userId );
			return mandate;
		}

		String message = String.format("Unable to find any mandate for notification detail for delegate=%s with mandateId=%s iun=%s", userId, mandateId, iun);
		handlePnMandateInvalid(message);
		return null;
	}

	private void handlePnMandateInvalid(String message) {
		log.error(message);
		throw new PnMandateNotFoundException(message);
	}

	private void notifyNotificationViewedEvent(InternalNotification notification, int recipientIndex, NotificationViewDelegateInfo delegateInfo, InternalAuthHeader internalAuthHeader) {
		String iun = notification.getIun();
		log.info("Send \"notification acknowlwdgement\" event for iun={}", iun);
		Instant createdAt = clock.instant();
		notificationAcknowledgementProducer.sendNotificationViewed( iun, createdAt, recipientIndex, delegateInfo, internalAuthHeader.xPagopaPnSrcCh(), internalAuthHeader.xPagopaPnSrcChDetails() );
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

	public void checkIfNotificationIsNotCancelled(String iun) {
		// recuperare tutta la timeline per controllare lo stato di richiesta è inefficente
		LegalNotificationDetail notificationDetail = getNotificationInformation(iun, true, false, null);
		InternalNotification notification = notificationDetail.getNotification();
		if(isNotificationCancelled(notificationDetail,notification.getIun())) {
			throw new PnNotificationNotFoundException(String.format("Notification with iun: %s has a request for cancellation", iun));
		}
	}
}
