package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestCheckAarDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ResponseCheckAarDto;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationQREntityDao;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class NotificationQRServiceTest {
    private static final String IUN = "FAKE-FAKE-FAKE-202209-F-1";
    @Mock
    private NotificationQREntityDao notificationQREntityDao;
    private NotificationQRService svc;

    @BeforeEach
    void setup() {
        svc = new NotificationQRService( notificationQREntityDao );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationByQRSuccess() {

        RequestCheckAarDto requestCheckAarDto = RequestCheckAarDto.builder()
                .recipientType( "PF" )
                .recipientInternalId( "recipientInternalId" )
                .aarQrCodeValue( "fakeAARQRCodeValue" )
                .build();

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue( "fakeAARQRCodeValue" )
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
                .recipientType( "PF" )
                .recipientInternalId( "recipientInternalId" )
                .aarQrCodeValue( "fakeAARQRCodeValue" )
                .build();

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.empty() );

        Executable todo = () -> svc.getNotificationByQR( requestCheckAarDto );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationQRFailureInvalidRecipientId() {
        RequestCheckAarDto requestCheckAarDto = RequestCheckAarDto.builder()
                .recipientType( "PF" )
                .recipientInternalId( "invalidRecipientInternalId" )
                .aarQrCodeValue( "fakeAARQRCodeValue" )
                .build();

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue( "fakeAARQRCodeValue" )
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

        Mockito.when( notificationQREntityDao.getQR(IUN) ).thenReturn(Map.of("internalId","qrCode"));

        Map<String, String> response = svc.getNotificationQR( IUN );

        Assertions.assertNotNull( response );
        Assertions.assertEquals( Set.of("internalId"), response.keySet() );
        Assertions.assertTrue(response.containsValue("qrCode") );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationQRFailure() {
      Mockito.when( notificationQREntityDao.getQR(IUN) ).thenReturn(Map.of());

        Executable todo = () -> svc.getNotificationQR( IUN );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }


}
