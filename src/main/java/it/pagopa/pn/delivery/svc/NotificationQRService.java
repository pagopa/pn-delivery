package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.utils.qr.QrUrlCodecService;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnIoMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.RequestCheckQrMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ResponseCheckQrMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.UserInfo;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationQREntityDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_AAR_QR_CODE;

@Service
@Slf4j
public class NotificationQRService {
    private final NotificationQREntityDao notificationQREntityDao;
    private final NotificationDao notificationDao;
    private final PnMandateClientImpl mandateClient;
    private final QrUrlCodecService qrUrlCodecService;
    private final PnExternalRegistriesClientImpl pnExternalRegistriesClient;

    public NotificationQRService(NotificationQREntityDao notificationQREntityDao, NotificationDao notificationDao, PnMandateClientImpl mandateClient, QrUrlCodecService qrUrlCodecService, PnExternalRegistriesClientImpl pnExternalRegistriesClient) {
        this.notificationQREntityDao = notificationQREntityDao;
        this.notificationDao = notificationDao;
        this.mandateClient = mandateClient;
        this.qrUrlCodecService = qrUrlCodecService;
        this.pnExternalRegistriesClient = pnExternalRegistriesClient;
    }

    public ResponseCheckAarDto getNotificationByQR( RequestCheckAarDto request ) {
        String aarQrCodeValue;
        if ( request.getAarQrCodeValue().matches("^(https)://.*$") ) {
            try {
                aarQrCodeValue = getAarQrCodeValue(request.getAarQrCodeValue());
            } catch (URISyntaxException | NullPointerException ex ) {
                throw new PnNotificationNotFoundException( "Notification not found",
                        String.format("Unable to parse aarQrCodeValue=%s",request.getAarQrCodeValue()),
                        ERROR_CODE_DELIVERY_UNSUPPORTED_AAR_QR_CODE,
                        ex);
            }
        } else {
            aarQrCodeValue = request.getAarQrCodeValue();
        }
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

    public UserInfoQrCode getAarQrCodeToDecode(RequestDecodeQrDto request) {
        String aarQrCodeValue = request.getAarQrCodeValue();
        log.info("Get QRCode value for aarQrCodeValue={}", aarQrCodeValue);
        InternalNotificationQR internalNotificationQR = getInternalNotificationQR(aarQrCodeValue);
        Optional<InternalNotification> optInternalNotification = notificationDao.getNotificationByIun(internalNotificationQR.getIun(), true);
        InternalNotification internalNotification = optInternalNotification.orElseThrow();
        String recipientInternalId = internalNotificationQR.getRecipientInternalId();
        Optional<NotificationRecipient> optNotificationRecipient = findRecipientByInternalId(internalNotification, recipientInternalId);
        NotificationRecipient notificationRecipient = optNotificationRecipient.orElseThrow();
        return buildUserInfoQrCode(internalNotificationQR, getUserInfo(notificationRecipient));

    }

    private UserInfoQrCode buildUserInfoQrCode(InternalNotificationQR internalNotificationQR, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.UserInfo userInfo) {
        return UserInfoQrCode.builder().build()
                .iun(internalNotificationQR.getIun())
                .recipientInfo(userInfo);
    }

    private it.pagopa.pn.delivery.generated.openapi.server.v1.dto. UserInfo getUserInfo(NotificationRecipient notificationRecipient) {
        String taxId = notificationRecipient.getTaxId();
        String denomination = notificationRecipient.getDenomination();
        it.pagopa.pn.delivery.generated.openapi.server.v1.dto.UserInfo userInfo = new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.UserInfo();
        userInfo.setTaxId(taxId);
        userInfo.setDenomination(denomination);
        return userInfo;
    }

    private String getAarQrCodeValue(String stringURI) throws URISyntaxException {
        URI uri = new URI(stringURI);
        return uri.getQuery().split("=")[1];
    }

    public ResponseCheckAarMandateDto getNotificationByQRWithMandate( RequestCheckAarMandateDto request, String recipientType, String userId, List<String> cxGroups ) {
        String aarQrCodeValue = request.getAarQrCodeValue();
        log.info( "Get notification QR with mandate for aarQrCodeValue={} recipientType={} userId={}", aarQrCodeValue, recipientType, userId);
        InternalNotificationQR internalNotificationQR = getInternalNotificationQR( aarQrCodeValue );
        Optional<InternalNotification> optInternalNotification = notificationDao.getNotificationByIun(internalNotificationQR.getIun(), false);
        if(optInternalNotification.isEmpty()) {
            log.info( "No notification found for iun={}", internalNotificationQR.getIun() );
            throw new PnNotificationNotFoundException( String.format( "No notification found for iun=%s", internalNotificationQR.getIun() ) );
        }

        InternalNotification internalNotification = optInternalNotification.get();
        boolean isRecipient = isRecipientInNotification( internalNotificationQR, userId );
        if (!isRecipient || (CxTypeAuthFleet.valueOf(recipientType).equals(CxTypeAuthFleet.PG) && !CollectionUtils.isEmpty(cxGroups))) {
            String mandateId = getMandateId( internalNotification.getSenderPaId(), internalNotificationQR, userId, CxTypeAuthFleet.valueOf(recipientType), cxGroups );
            if ( StringUtils.hasText( mandateId ) ) {
                return ResponseCheckAarMandateDto.builder()
                        .iun( internalNotificationQR.getIun() )
                        .mandateId( mandateId )
                        .build();
            } else {
                log.info( "Invalid userId={} without mandate for aarQrCodeValue={}", userId, aarQrCodeValue );
                throw new PnNotificationNotFoundException( String.format("Invalid userId=%s without mandate for aarQrCodeValue=%s", userId, aarQrCodeValue) );
            }
        }
        else {

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

    private Optional<NotificationRecipient> findRecipientByInternalId(InternalNotification internalNotification, String recipientInternalId) {
        return internalNotification.getRecipients().stream()
                .filter(recipientId -> StringUtils.hasText(recipientId.getInternalId()) && recipientId.getInternalId().equals(recipientInternalId))
                .findFirst();
    }

    private String getMandateId(String senderPaId, InternalNotificationQR internalNotificationQR, String userId, CxTypeAuthFleet cxType, List<String> cxGroups) {
        String mandateId = null;
        String rootSenderId = pnExternalRegistriesClient.getRootSenderId(senderPaId);
        List<InternalMandateDto> mandateDtoList = mandateClient.listMandatesByDelegate(userId, null, cxType, cxGroups);
        if (!CollectionUtils.isEmpty(mandateDtoList)) {
            Optional<InternalMandateDto> optMandate = mandateDtoList.stream()
                    .filter(mandate -> userId.equals(mandate.getDelegate()) &&
                            internalNotificationQR.getRecipientInternalId().equals( mandate.getDelegator() ) &&
                            checkVisibilityId(mandate.getVisibilityIds(), rootSenderId)
                    )
                    .findFirst();
            if ( optMandate.isPresent() ) {
                mandateId = optMandate.get().getMandateId();
            }
        }
        return mandateId;
    }

    // If visibilityIds is null or empty, the mandate is visible to all. When is present, it must contain the senderPaId to be visible to the user
    private boolean checkVisibilityId(List<String> visibilityIds, String senderPaId) {
        return CollectionUtils.isEmpty(visibilityIds) || visibilityIds.contains(senderPaId);
    }

    public ResponseCheckQrMandateDto getNotificationByQRFromIOWithMandate(RequestCheckQrMandateDto request, String recipientType, String userId, List<String> cxGroups ) {
        String aarQrCodeValue = decodeQrUrl(request);
        log.info( "Get notification QR from IO with mandate for aarQrCodeValue={} recipientType={} userId={}", aarQrCodeValue, recipientType, userId);
        InternalNotificationQR internalNotificationQR = getInternalNotificationQR( aarQrCodeValue );
        String recipientInternalId = internalNotificationQR.getRecipientInternalId();

        Optional<InternalNotification> optInternalNotification = notificationDao.getNotificationByIun(internalNotificationQR.getIun(), true);
        if(optInternalNotification.isEmpty()) {
            log.info( "No notification by iun={} ", internalNotificationQR.getIun());
            throw new PnNotificationNotFoundException( String.format( "No notification by iun=%s", internalNotificationQR.getIun() ) );
        }
        InternalNotification internalNotification = optInternalNotification.get();

        boolean isRecipient = isRecipientInNotification( internalNotificationQR, userId );
        if (!isRecipient || (CxTypeAuthFleet.valueOf(recipientType).equals(CxTypeAuthFleet.PG) && !CollectionUtils.isEmpty(cxGroups))) {
            String mandateId = getMandateId( internalNotification.getSenderPaId(), internalNotificationQR, userId, CxTypeAuthFleet.valueOf(recipientType), cxGroups );
            if ( StringUtils.hasText( mandateId ) ) {
                return buildResponseCheckAarMandateDto(internalNotification, recipientInternalId, mandateId);
            } else {
                log.info( "Invalid userId={} without mandate for aarQrCodeValue={}", userId, aarQrCodeValue );
                ResponseCheckQrMandateDto responseCheckQrMandateDto = buildResponseCheckAarMandateDto(internalNotification, recipientInternalId, null);
                throw new PnIoMandateNotFoundException(responseCheckQrMandateDto);
            }
        } else {
            log.info( "userId={} is recipient of notification with iun={}", userId, internalNotification.getIun());
            return buildResponseCheckAarMandateDto(internalNotification, recipientInternalId, null);
        }
    }

    private String decodeQrUrl(RequestCheckQrMandateDto request) {
        try {
            return qrUrlCodecService.decode(request.getAarQrCodeValue());
        } catch (Exception ex) {
            throw new PnBadRequestException("Invalid QR code format",
                    String.format("Unable to decode aarQrCodeValue=%s", request.getAarQrCodeValue()),
                    ERROR_CODE_DELIVERY_UNSUPPORTED_AAR_QR_CODE, ex);
        }
    }

    private ResponseCheckQrMandateDto buildResponseCheckAarMandateDto(InternalNotification notification, String internalRecipientId, String mandateId) {
        NotificationRecipient recipient = notification.getRecipients()
                .stream()
                .filter(rec -> rec.getInternalId().equals(internalRecipientId))
                .findFirst()
                .orElseThrow(() -> new PnNotificationNotFoundException(String.format("No recipient found with internalId=%s for notification iun=%s", internalRecipientId, notification.getIun())));

        UserInfo recipientUserInfo = UserInfo.builder()
                .denomination(recipient.getDenomination())
                .taxId(recipient.getTaxId())
                .build();

        return ResponseCheckQrMandateDto.builder()
                .iun(notification.getIun())
                .mandateId(mandateId)
                .recipientInfo(recipientUserInfo)
                .build();
    }
}
