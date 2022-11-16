package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestCheckAarDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestCheckAarMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ResponseCheckAarDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ResponseCheckAarMandateDto;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationQREntityDao;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class NotificationQRService {
    private final NotificationQREntityDao notificationQREntityDao;
    private final PnMandateClientImpl mandateClient;

    public NotificationQRService(NotificationQREntityDao notificationQREntityDao, PnMandateClientImpl mandateClient) {
        this.notificationQREntityDao = notificationQREntityDao;
        this.mandateClient = mandateClient;
    }

    public ResponseCheckAarDto getNotificationByQR( RequestCheckAarDto request ) {
        String aarQrCodeValue = request.getAarQrCodeValue();
        String recipientType = request.getRecipientType();
        String recipientInternalId = request.getRecipientInternalId();
        log.info( "Get notification QR for aarQrCodeValue={} recipientType={} recipientInternalId={}", aarQrCodeValue, recipientType, recipientInternalId);
        InternalNotificationQR internalNotificationQR = getInternalNotificationQR( aarQrCodeValue );
        if ( isRecipientInNotification( internalNotificationQR, recipientInternalId ) ) {
            return ResponseCheckAarDto.builder()
                    .iun( internalNotificationQR.getIun() )
                    .build();
        } else {
            log.info( "Invalid recipientInternalId={} for aarQrCodeValue={} recipientType={}", recipientInternalId, aarQrCodeValue, recipientType );
            throw new PnNotificationNotFoundException( String.format( "Invalid recipientInternalId=%s for aarQrCodeValue=%s recipientType=%s", recipientInternalId, aarQrCodeValue, recipientType) );
        }
    }

    public ResponseCheckAarMandateDto getNotificationByQRWithMandate( RequestCheckAarMandateDto request, String recipientType, String userId ) {
        String aarQrCodeValue = request.getAarQrCodeValue();
        log.info( "Get notification QR with mandate for aarQrCodeValue={} recipientType={} userId={}", aarQrCodeValue, recipientType, userId);
        InternalNotificationQR internalNotificationQR = getInternalNotificationQR( aarQrCodeValue );
        if ( !isRecipientInNotification( internalNotificationQR, userId ) ){
            String mandateId = getMandateId( internalNotificationQR, userId );
            if ( StringUtils.hasText( mandateId ) ) {
                return ResponseCheckAarMandateDto.builder()
                        .iun( internalNotificationQR.getIun() )
                        .mandateId( mandateId )
                        .build();
            } else {
                log.info( "Invalid userId={} without mandate for aarQrCodeValue={}", userId, aarQrCodeValue );
                throw new PnNotificationNotFoundException( String.format("Invalid userId=%s without mandate for aarQrCodeValue=%s", userId, aarQrCodeValue) );
            }
        } else {
            return ResponseCheckAarMandateDto.builder()
                    .iun( internalNotificationQR.getIun() )
                    .build();
        }
    }

    public Map<String, String> getQRByIun(String iun) {
        log.info( "Get notification QR for iun={}", iun);
        Map<String, String> qrMap = notificationQREntityDao.getQRByIun( iun );
        if(qrMap.isEmpty()) {
            log.info( "No notification by iun={} ", iun);
            throw new PnNotificationNotFoundException( String.format( "No notification by iun=%s", iun ) );
        }
        return qrMap;
    }

    private InternalNotificationQR getInternalNotificationQR( String aarQrCodeValue ) {
        Optional<InternalNotificationQR> optionalInternalNotificationQR = notificationQREntityDao.getNotificationByQR( aarQrCodeValue );
        if (optionalInternalNotificationQR.isPresent()) {
            return optionalInternalNotificationQR.get();
        } else {
            log.info( "No notification by aarQrCodeValue={}", aarQrCodeValue );
            throw new PnNotificationNotFoundException( String.format( "No notification by aarQrCodeValue=%s", aarQrCodeValue ) );
        }
    }

    private boolean isRecipientInNotification(InternalNotificationQR internalNotificationQR, String recipientInternalId) {
        return internalNotificationQR.getRecipientInternalId().equals( recipientInternalId );
    }

    private String getMandateId( InternalNotificationQR internalNotificationQR, String userId ) {
        String mandateId = null;
        List<InternalMandateDto> mandateDtoList = mandateClient.listMandatesByDelegate(userId, null);
        if (!CollectionUtils.isEmpty(mandateDtoList)) {
            Optional<InternalMandateDto> optMandate = mandateDtoList.stream()
                    .filter(mandate -> userId.equals(mandate.getDelegate()))
                    .findFirst();
            if (optMandate.isPresent() && isRecipientInNotification(internalNotificationQR, optMandate.get().getDelegator())) {
                mandateId = optMandate.get().getMandateId();
            }
        }
        return mandateId;
    }
}
