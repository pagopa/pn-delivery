package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestCheckAarDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ResponseCheckAarDto;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationQREntityDao;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class NotificationQRService {
    private final NotificationQREntityDao notificationQREntityDao;

    public NotificationQRService(NotificationQREntityDao notificationQREntityDao) {
        this.notificationQREntityDao = notificationQREntityDao;
    }

    public ResponseCheckAarDto getNotificationByQR(RequestCheckAarDto request) {
        String aarQrCodeValue = request.getAarQrCodeValue();
        String recipientType = request.getRecipientType();
        String recipientInternalId = request.getRecipientInternalId();
        log.info( "Get notification QR for aarQrCodeValue={} recipientType={} recipientInternalId={}", aarQrCodeValue, recipientType, recipientInternalId);
        Optional<InternalNotificationQR> optionalInternalNotificationQR = notificationQREntityDao.getNotificationByQR( aarQrCodeValue );
        if (optionalInternalNotificationQR.isPresent()) {
            InternalNotificationQR internalNotificationQR = optionalInternalNotificationQR.get();
            if ( internalNotificationQR.getRecipientInternalId().equals( recipientInternalId ) ) {
                return ResponseCheckAarDto.builder()
                        .iun( internalNotificationQR.getIun() )
                        .build();
            } else {
                log.info( "Invalid recipientInternalId={} for aarQrCodeValue={} recipientType={}", recipientInternalId, aarQrCodeValue, recipientType );
                throw new PnNotificationNotFoundException( String.format( "Invalid recipientInternalId=%s for aarQrCodeValue=%s recipientType=%s", recipientInternalId, aarQrCodeValue, recipientType) );
            }
        } else {
            log.info( "No notification by aarQrCodeValue={} recipientType={} recipientInternalId={}", aarQrCodeValue, recipientType, recipientInternalId );
            throw new PnNotificationNotFoundException( String.format( "No notification by aarQrCodeValue=%s recipientType=%s recipientInternalId=%s", aarQrCodeValue, recipientType, recipientInternalId ) );
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
}
