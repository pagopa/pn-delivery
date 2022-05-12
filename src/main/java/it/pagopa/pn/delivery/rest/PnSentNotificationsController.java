package it.pagopa.pn.delivery.rest;


import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadB2BApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadWebApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
public class PnSentNotificationsController implements SenderReadB2BApi,SenderReadWebApi {

    private final NotificationRetrieverService retrieveSvc;
    private final PnDeliveryConfigs cfg;
    private final ModelMapperFactory modelMapperFactory;
    public static final String VALIDATION_ERROR_STATUS = "Validation error";

    public PnSentNotificationsController(NotificationRetrieverService retrieveSvc, PnDeliveryConfigs cfg, ModelMapperFactory modelMapperFactory) {
        this.retrieveSvc = retrieveSvc;
        this.cfg = cfg;
        this.modelMapperFactory = modelMapperFactory;
    }

    @Override
    public ResponseEntity<FullSentNotification> getSentNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String iun) {
        InternalNotification internalNotification = retrieveSvc.getNotificationInformation( iun, true );
        ModelMapper mapper = modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class );
        FullSentNotification result = mapper.map( internalNotification, FullSentNotification.class );
        return ResponseEntity.ok( result );
    }



    @Override
    public ResponseEntity<NotificationSearchResponse> searchSentNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, Date startDate, Date endDate, String recipientId, NotificationStatus status, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey) {
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(true)
                .senderReceiverId(xPagopaPnCxId)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .filterId(recipientId)
                .status(status)
                //.groups( groups != null ? Arrays.asList( groups ) : null )
                .subjectRegExp(subjectRegExp)
                .iunMatch(iunMatch)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();

        ResultPaginationDto<NotificationSearchRow,String> serviceResult =  retrieveSvc.searchNotification( searchDto );

        ModelMapper mapper = modelMapperFactory.createModelMapper(ResultPaginationDto.class, NotificationSearchResponse.class );
        NotificationSearchResponse response = mapper.map( serviceResult, NotificationSearchResponse.class );
        return ResponseEntity.ok( response );
    }

    /*@GetMapping( PnDeliveryRestConstants.NOTIFICATION_SENT_DOCUMENTS_PATH)
    public ResponseEntity<Resource> getSentNotificationDocument(
            @RequestHeader(name = PnDeliveryRestConstants.CX_ID_HEADER ) String paId,
            @PathVariable("iun") String iun,
            @PathVariable("documentIndex") int documentIndex,
            ServerHttpResponse response
    ) {
        if(cfg.isDownloadWithPresignedUrl()) {
            String redirectUrl = retrieveSvc.downloadDocumentWithRedirect(iun, documentIndex);
            //response.setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
            //response.getHeaders().setLocation(URI.create( redirectUrl ));
            //return null;
            response.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            String responseString  = "{ \"url\": \"" + redirectUrl + "\"}";
            Resource resource = new ByteArrayResource( responseString.getBytes(StandardCharsets.UTF_8) );
            return ResponseEntity.ok( resource );
        } else {
            ResponseEntity<Resource> resource = retrieveSvc.downloadDocument(iun, documentIndex);
            return AttachmentRestUtils.prepareAttachment( resource, iun, "doc" + documentIndex );
        }

    }*/

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex){
        return HandleValidation.handleValidationException(ex, VALIDATION_ERROR_STATUS);
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return SenderReadB2BApi.super.getRequest();
    }

    @Override
    public ResponseEntity<NewNotificationRequestStatusResponse> getNotificationRequestStatus(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String requestId, String paProtocolNumber, String idempotenceToken) {
        return SenderReadB2BApi.super.getNotificationRequestStatus(xPagopaPnUid, xPagopaPnCxType, xPagopaPnCxId, xPagopaPnCxGroups, requestId, paProtocolNumber, idempotenceToken);
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentNotificationAttachment(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String iun, BigDecimal recipientIdx, String attachmentName) {
        return SenderReadB2BApi.super.getSentNotificationAttachment(xPagopaPnUid, xPagopaPnCxType, xPagopaPnCxId, xPagopaPnCxGroups, iun, recipientIdx, attachmentName);
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentNotificationDocument(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String iun, BigDecimal docIdx) {
        NotificationAttachmentDownloadMetadataResponse.NotificationAttachmentDownloadMetadataResponseBuilder responseBuilder = NotificationAttachmentDownloadMetadataResponse.builder();

        String redirectUrl = retrieveSvc.downloadDocumentWithRedirect(iun, docIdx.intValue());
        //String responseString  = "{ \"url\": \"" + redirectUrl + "\"}";
        //Resource resource = new ByteArrayResource( responseString.getBytes(StandardCharsets.UTF_8) );
        // TODO info mancanti di NotificationAttachmentDonwnloadMetadataResponse quando get file da safe-storage
        NotificationAttachmentDownloadMetadataResponse response = responseBuilder.url(redirectUrl).build();
        return ResponseEntity.ok( response );
    }
}
