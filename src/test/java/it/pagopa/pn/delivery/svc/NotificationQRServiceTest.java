package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationQREntityDao;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

class NotificationQRServiceTest {
    private static final String IUN = "FAKE-FAKE-FAKE-202209-F-1";
    public static final String AAR_QR_CODE_VALUE = "fakeAARQRCodeValue";
    public static final String URL_AAR_QR_VALUE = "https://fake.domain.com/notifica?aar=fakeAARQRCodeValue";
    public static final String INVALID_URL_NO_QUERY_AAR_QR_VALUE = "https://invalid.domain.com/notifica";
    public static final String INVALID_URL_AAR_QR_VALUE = "https://invalid.domain.com/notifica?ciccioPasticcio=ciao&aar=fake_aar";
    public static final String RECIPIENT_TYPE = "PF";

    @Mock
    private NotificationQREntityDao notificationQREntityDao;
    @Mock
    private PnMandateClientImpl mandateClient;
    private NotificationQRService svc;

    @BeforeEach
    void setup() {
        svc = new NotificationQRService( notificationQREntityDao, mandateClient);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByQRSuccess() {

        RequestCheckAarDto requestCheckAarDto = RequestCheckAarDto.builder()
                .recipientType(RECIPIENT_TYPE)
                .recipientInternalId( "recipientInternalId" )
                .aarQrCodeValue(AAR_QR_CODE_VALUE)
                .build();

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue(AAR_QR_CODE_VALUE)
                .iun( "iun" )
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .recipientInternalId( "recipientInternalId" )
                .build();

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.of( internalNotificationQR ) );

        ResponseCheckAarDto response = svc.getNotificationByQR( requestCheckAarDto );

        Assertions.assertNotNull( response );
        Assertions.assertEquals( "iun", response.getIun() );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByUrlQRSuccess() {

        RequestCheckAarDto requestCheckAarDto = RequestCheckAarDto.builder()
                .recipientType(RECIPIENT_TYPE)
                .recipientInternalId( "recipientInternalId" )
                .aarQrCodeValue(URL_AAR_QR_VALUE)
                .build();

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue(AAR_QR_CODE_VALUE)
                .iun( "iun" )
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .recipientInternalId( "recipientInternalId" )
                .build();

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.of( internalNotificationQR ) );

        ResponseCheckAarDto response = svc.getNotificationByQR( requestCheckAarDto );

        Assertions.assertNotNull( response );
        Assertions.assertEquals( "iun", response.getIun() );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByQRFailure() {
        RequestCheckAarDto requestCheckAarDto = RequestCheckAarDto.builder()
                .recipientType(RECIPIENT_TYPE)
                .recipientInternalId( "recipientInternalId" )
                .aarQrCodeValue(AAR_QR_CODE_VALUE)
                .build();

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.empty() );

        Executable todo = () -> svc.getNotificationByQR( requestCheckAarDto );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByQRInvalidURINoQueryFailure() {
        RequestCheckAarDto requestCheckAarDto = RequestCheckAarDto.builder()
                .recipientType(RECIPIENT_TYPE)
                .recipientInternalId( "recipientInternalId" )
                .aarQrCodeValue( INVALID_URL_NO_QUERY_AAR_QR_VALUE )
                .build();

        Executable todo = () -> svc.getNotificationByQR( requestCheckAarDto );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByQRInvalidURIFailure() {
        RequestCheckAarDto requestCheckAarDto = RequestCheckAarDto.builder()
                .recipientType(RECIPIENT_TYPE)
                .recipientInternalId( "recipientInternalId" )
                .aarQrCodeValue( INVALID_URL_AAR_QR_VALUE )
                .build();

        Executable todo = () -> svc.getNotificationByQR( requestCheckAarDto );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationQRFailureInvalidRecipientId() {
        RequestCheckAarDto requestCheckAarDto = RequestCheckAarDto.builder()
                .recipientType(RECIPIENT_TYPE)
                .recipientInternalId( "invalidRecipientInternalId" )
                .aarQrCodeValue(AAR_QR_CODE_VALUE)
                .build();

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue(AAR_QR_CODE_VALUE)
                .iun( "iun" )
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .recipientInternalId( "recipientInternalId" )
                .build();

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.of( internalNotificationQR ) );

        Executable todo = () -> svc.getNotificationByQR( requestCheckAarDto );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }
    
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationQRSuccess() {

        Mockito.when( notificationQREntityDao.getQRByIun(IUN) ).thenReturn(Map.of("internalId","qrCode"));

        Map<String, String> response = svc.getQRByIun( IUN );

        Assertions.assertNotNull( response );
        Assertions.assertEquals( Set.of("internalId"), response.keySet() );
        Assertions.assertTrue(response.containsValue("qrCode") );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationQRFailure() {
      Mockito.when( notificationQREntityDao.getQRByIun(IUN) ).thenReturn(Map.of());

        Executable todo = () -> svc.getQRByIun( IUN );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByQRWithMandateByRecipientSuccess() {
        // Given
        String userId = "recipientInternalId";
        RequestCheckAarMandateDto request = new RequestCheckAarMandateDto( AAR_QR_CODE_VALUE );

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue(AAR_QR_CODE_VALUE)
                .iun( "iun" )
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .recipientInternalId( "recipientInternalId" )
                .build();

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.of( internalNotificationQR ) );

        // When
        ResponseCheckAarMandateDto result = svc.getNotificationByQRWithMandate( request, RECIPIENT_TYPE, userId );

        // Then
        Assertions.assertNotNull( result );
        Assertions.assertEquals( "iun", result.getIun() );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByQRWithMandateByDelegateSuccess() {
        // Given
        String userId = "delegateInternalId";
        RequestCheckAarMandateDto request = new RequestCheckAarMandateDto( AAR_QR_CODE_VALUE );

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue(AAR_QR_CODE_VALUE)
                .iun( "iun" )
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .recipientInternalId( "recipientInternalId" )
                .build();

        InternalMandateDto internalMandateDto = new InternalMandateDto()
                .mandateId( "wrongMandateId" )
                .delegator( "otherRecipientInternalId" )
                .delegate( userId );

        InternalMandateDto internalMandateDto1 = new InternalMandateDto()
                .mandateId( "mandateId" )
                .delegator( "recipientInternalId" )
                .delegate( userId );

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.of( internalNotificationQR ) );
        Mockito.when( mandateClient.listMandatesByDelegate( userId, null ) ).thenReturn( List.of( internalMandateDto, internalMandateDto1 ) );

        // When
        ResponseCheckAarMandateDto result = svc.getNotificationByQRWithMandate( request, RECIPIENT_TYPE, userId );

        // Then
        Assertions.assertNotNull( result );
        Assertions.assertEquals( "iun", result.getIun() );
        Assertions.assertNotNull( result.getMandateId() );
        Assertions.assertEquals( "mandateId", result.getMandateId() );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByQRWithMandateByDelegateFailure() {
        // Given
        String userId = "delegateInternalId";
        RequestCheckAarMandateDto request = new RequestCheckAarMandateDto( AAR_QR_CODE_VALUE );

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue(AAR_QR_CODE_VALUE)
                .iun( "iun" )
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .recipientInternalId( "recipientInternalId" )
                .build();

        InternalMandateDto internalMandateDto = new InternalMandateDto()
                .mandateId( "mandateId" )
                .delegator( "recipientInternalId" )
                .delegate( "wrongDelegateInternalId" );

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.of( internalNotificationQR ) );
        Mockito.when( mandateClient.listMandatesByDelegate( userId, null ) ).thenReturn( List.of( internalMandateDto ) );

        // When
        Executable todo = () -> svc.getNotificationByQRWithMandate( request, RECIPIENT_TYPE, userId );

        // Then
        Assertions.assertThrows(PnNotificationNotFoundException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByQRWithMandateByDelegateNoMandateFailure() {
        // Given
        String userId = "delegateInternalId";
        RequestCheckAarMandateDto request = new RequestCheckAarMandateDto( AAR_QR_CODE_VALUE );

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue(AAR_QR_CODE_VALUE)
                .iun( "iun" )
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .recipientInternalId( "recipientInternalId" )
                .build();

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.of( internalNotificationQR ) );
        Mockito.when( mandateClient.listMandatesByDelegate( userId, null ) ).thenReturn( Collections.emptyList() );

        // When
        Executable todo = () -> svc.getNotificationByQRWithMandate( request, RECIPIENT_TYPE, userId );

        // Then
        Assertions.assertThrows(PnNotificationNotFoundException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByQRWithMandateWrongDelegatorFailure() {
        // Given
        String userId = "delegateInternalId";
        RequestCheckAarMandateDto request = new RequestCheckAarMandateDto( AAR_QR_CODE_VALUE );

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue(AAR_QR_CODE_VALUE)
                .iun( "iun" )
                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                .recipientInternalId( "recipientInternalId" )
                .build();

        InternalMandateDto internalMandateDto = new InternalMandateDto()
                .mandateId( "mandateId" )
                .delegator( "wrongRecipientInternalId" )
                .delegate( userId );

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.of( internalNotificationQR ) );
        Mockito.when( mandateClient.listMandatesByDelegate( userId, null ) ).thenReturn( List.of( internalMandateDto ) );

        // When
        Executable todo = () -> svc.getNotificationByQRWithMandate( request, RECIPIENT_TYPE, userId );

        // Then
        Assertions.assertThrows(PnNotificationNotFoundException.class, todo);
    }


}
