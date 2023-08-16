package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadB2BApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadWebApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.InternalFieldsCleaner;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService.InternalAttachmentWithFileKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED;


@RestController
@Slf4j
public class PnSentNotificationsController implements SenderReadB2BApi,SenderReadWebApi {

    private final NotificationRetrieverService retrieveSvc;
    private final NotificationAttachmentService notificationAttachmentService;
    private final ModelMapper modelMapper;

    public PnSentNotificationsController(NotificationRetrieverService retrieveSvc, NotificationAttachmentService notificationAttachmentService, ModelMapper modelMapper) {
        this.retrieveSvc = retrieveSvc;
        this.notificationAttachmentService = notificationAttachmentService;
        this.modelMapper = modelMapper;
    }

    @Override
    public ResponseEntity<FullSentNotification> getSentNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, List<String> xPagopaPnCxGroups) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VIEW_SND, "getSenderNotification")
                .iun(iun)
                .build();
        logEvent.log();
/*
        if ( NotificationStatus.IN_VALIDATION.equals( internalNotification.getNotificationStatus() )
                || NotificationStatus.REFUSED.equals( internalNotification.getNotificationStatus() ) ) {
            logEvent.generateFailure("Unable to find notification with iun={} cause status={}", internalNotification.getIun(), internalNotification.getNotificationStatus()).log();
            throw new PnNotificationNotFoundException( "Unable to find notification with iun="+ internalNotification.getIun() );
        }
        InternalFieldsCleaner.cleanInternalFields( internalNotification );
*/

        List<NotificationPaymentItem> listNotificationPayment = new ArrayList<>();

        NotificationPaymentItem item = NotificationPaymentItem.builder()
                .pagoPa(PagoPaPayment.builder()
                        .creditorTaxId("firstCreditorTaxId")
                        .noticeCode("firstNoticeCode")
                        .applyCost(true)
                        .attachment(
                                NotificationPaymentAttachment.builder()
                                        .ref( NotificationAttachmentBodyRef.builder().key("k1").versionToken("v1").build())
                                        .contentType("application/pdf")
                                        .digests( NotificationAttachmentDigests.builder().sha256("sha").build())
                                        .build()
                        )
                        .build())
                .build();

        NotificationPaymentItem item2 = NotificationPaymentItem.builder()
                .pagoPa(PagoPaPayment.builder()
                        .creditorTaxId("SecondCreditorTaxId")
                        .noticeCode("SecondNoticeCode")
                        .applyCost(false)
                        .attachment(
                                NotificationPaymentAttachment.builder()
                                        .ref( NotificationAttachmentBodyRef.builder().key("k2").versionToken("v2").build())
                                        .contentType("application/pdf")
                                        .digests( NotificationAttachmentDigests.builder().sha256("sha2").build())
                                        .build()
                        )
                        .build())
                .build();

        listNotificationPayment.add(item);
        listNotificationPayment.add(item2);
        
        FullSentNotification result = FullSentNotification.builder()
                .iun("IUN_01")
                .paProtocolNumber( "protocol_01" )
                .notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
                .paFee(100)
                .vat(200)
                .subject("Subject 01")
                .physicalCommunicationType( FullSentNotification.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER )
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .group( "Group_1" )
                .senderPaId( "pa_02" )
                .recipientIds(Collections.singletonList("Codice Fiscale 01"))
                .sourceChannel("sourceChannel")
                .sentAt( OffsetDateTime.now() )
                .notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .taxId("Codice Fiscale 01")
                        .denomination("Nome Cognome/Ragione Sociale")
                        .digitalDomicile(NotificationDigitalAddress.builder()
                                .type(NotificationDigitalAddress.TypeEnum.PEC)
                                .address("account@dominio.it")
                                .build())
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .physicalAddress( NotificationPhysicalAddress.builder()
                                .address( "address" )
                                .zip( "zip" )
                                .municipality( "municipality" )
                                .at( "at" )
                                .addressDetails( "addressDetails" )
                                .province( "province" )
                                .foreignState( "foreignState" )
                                .build() )
                        .payments(listNotificationPayment)
                        .build()
                ))
                .documents(Arrays.asList(
                        NotificationDocument.builder()
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key("key_doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .contentType( "application/pdf" )
                                .build(),
                        NotificationDocument.builder()
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key("key_doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .contentType( "application/pdf" )
                                .build()
                ))
                .build();
        
        logEvent.generateSuccess().log();
        return ResponseEntity.ok( result );
    }



    @Override
    public ResponseEntity<NotificationSearchResponse> searchSentNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, OffsetDateTime startDate, OffsetDateTime endDate, List<String> xPagopaPnCxGroups, String recipientId, NotificationStatus status, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_SEARCH_SND, "searchSentNotification")
                .iun(iunMatch)
                .build();
        logEvent.log();
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .bySender(true)
                .senderReceiverId(xPagopaPnCxId)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .filterId(recipientId)
                .statuses(status==null?List.of():List.of(status))
                .receiverIdIsOpaque(false)
                .groups( xPagopaPnCxGroups )
                .subjectRegExp(subjectRegExp)
                .iunMatch(iunMatch)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();
        ResultPaginationDto<NotificationSearchRow,String> serviceResult;
        NotificationSearchResponse response = new NotificationSearchResponse();
        try {
            serviceResult = retrieveSvc.searchNotification(searchDto, null, null);
            response = modelMapper.map( serviceResult, NotificationSearchResponse.class );
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok( response );
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return SenderReadB2BApi.super.getRequest();
    }

    @Override
    public ResponseEntity<NewNotificationRequestStatusResponse> getNotificationRequestStatus(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String notificationRequestId, String paProtocolNumber, String idempotenceToken) {
        InternalNotification internalNotification;
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before( PnAuditLogEventType.AUD_NT_CHECK, "getNotificationRequestStatus notificationRequestId={} paProtocolNumber={} idempotenceToken={}",
                        notificationRequestId,
                        paProtocolNumber,
                        idempotenceToken)
                .build();
        logEvent.log();
        if (StringUtils.hasText( notificationRequestId )) {
            String iun = new String(Base64Utils.decodeFromString(notificationRequestId), StandardCharsets.UTF_8);
            logEvent.getMdc().put("iun", iun);
            internalNotification = retrieveSvc.getNotificationInformationWithSenderIdCheck( iun, xPagopaPnCxId, xPagopaPnCxGroups );
        } else {
            if ( !StringUtils.hasText( paProtocolNumber ) ) {
                PnInvalidInputException e = new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, "paProtocolNumber");
                logEvent.generateFailure("[notificationRequestId={} idempotenceToken={}]" + e.getProblem(), notificationRequestId, idempotenceToken);
                throw e;
            }
            if (!StringUtils.hasText( idempotenceToken ) ) {
                PnInvalidInputException e = new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, "idempotenceToken");
                logEvent.generateFailure("[notificationRequestId={} paProtocolNumber={}]" + e.getProblem(), notificationRequestId, paProtocolNumber);
                throw e;
            }
            internalNotification = retrieveSvc.getNotificationInformation( xPagopaPnCxId, paProtocolNumber, idempotenceToken, xPagopaPnCxGroups);
        }
        InternalFieldsCleaner.cleanInternalFields( internalNotification );
        NewNotificationRequestStatusResponse response = modelMapper.map(
                internalNotification,
                NewNotificationRequestStatusResponse.class
        );
        response.setNotificationRequestId( Base64Utils.encodeToString( internalNotification.getIun().getBytes(StandardCharsets.UTF_8) ));

        NotificationStatus lastStatus;
        if ( !CollectionUtils.isEmpty( internalNotification.getNotificationStatusHistory() )) {
            lastStatus = internalNotification.getNotificationStatusHistory().get(
                    internalNotification.getNotificationStatusHistory().size() - 1 ).getStatus();
        } else {
            log.debug( "No status history for notificationRequestId={}", notificationRequestId );
            lastStatus = NotificationStatus.IN_VALIDATION;
        }

        switch (lastStatus) {
            case IN_VALIDATION -> {
                response.setNotificationRequestStatus("WAITING");
                response.retryAfter(10);
                response.setIun(null);
            }
            case REFUSED -> {
                response.setNotificationRequestStatus("REFUSED");
                response.setIun(null);
                Optional<TimelineElement> timelineElement = internalNotification.getTimeline().stream().filter(
                        tle -> TimelineElementCategory.REQUEST_REFUSED.equals(tle.getCategory())).findFirst();
                timelineElement.ifPresent(element -> setRefusedErrors(response, element));
            }
            default -> response.setNotificationRequestStatus("ACCEPTED");
        }

        logEvent.generateSuccess().log();
        return ResponseEntity.ok( response );
    }

    private void setRefusedErrors(NewNotificationRequestStatusResponse response, TimelineElement timelineElement) {
        List<NotificationRefusedError> refusalReasons = timelineElement.getDetails().getRefusalReasons();
        List<ProblemError> problemErrorList = refusalReasons.stream().map(
                reason -> ProblemError.builder()
                        .code( reason.getErrorCode() )
                        .detail( reason.getDetail() )
                        .build()
        ).toList();
        response.setErrors( problemErrorList );
    }
    
    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentNotificationDocument(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, Integer docIdx, List<String> xPagopaPnCxGroups) {
        InternalAttachmentWithFileKey internalAttachmentWithFileKey = new InternalAttachmentWithFileKey();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_DOCOPEN_SND, "getSentNotificationDocument={}", docIdx)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnUid, xPagopaPnCxGroups);
            internalAttachmentWithFileKey = notificationAttachmentService.downloadDocumentWithRedirectWithFileKey(
                    iun,
                    internalAuthHeader,
                    null,
                    docIdx,
                    false
            );
            if(internalAttachmentWithFileKey == null || internalAttachmentWithFileKey.getFileKey() == null){
                logEvent.generateSuccess().log();
            }else{
                logEvent.getMdc().put("dockey", internalAttachmentWithFileKey.getFileKey());
                logEvent.generateSuccess().log();
            }
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok( internalAttachmentWithFileKey == null ? null : internalAttachmentWithFileKey.getDownloadMetadataResponse() );
    }
}
