package it.pagopa.pn.delivery.rest;


import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
public class PnSentNotificationsController implements SenderReadApi {

    private final NotificationRetrieverService retrieveSvc;
    private final PnDeliveryConfigs cfg;
    private final ModelMapperFactory modelMapperFactory;
    public static final String VALIDATION_ERROR_STATUS = "Validation error";

    public PnSentNotificationsController(NotificationRetrieverService retrieveSvc, PnDeliveryConfigs cfg, ModelMapperFactory modelMapperFactory) {
        this.retrieveSvc = retrieveSvc;
        this.cfg = cfg;
        this.modelMapperFactory = modelMapperFactory;
    }

    /*@GetMapping(PnDeliveryRestConstants.NOTIFICATION_SENT_PATH)
    @JsonView(value = NotificationJsonViews.Sent.class )
    public Notification getSentNotification(
            @RequestHeader(name = PnDeliveryRestConstants.CX_ID_HEADER ) String paId,
            @PathVariable( name = "iun") String iun,
            @RequestParam( name = "with_timeline", defaultValue = "true", required = false ) boolean withTimeline
    ) {
            return retrieveSvc.getNotificationInformation( iun, withTimeline );
    }*/

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

}
