package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.NotificationRecipientType;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NotificationQRServiceTest {

    @Mock
    private NotificationQREntityDao notificationQREntityDao;
    private NotificationQRService svc;

    @BeforeEach
    void setup() {
        svc = new NotificationQRService( notificationQREntityDao );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationQRSuccess() {

        RequestCheckAarDto requestCheckAarDto = RequestCheckAarDto.builder()
                .recipientType( "PF" )
                .recipientInternalId( "recipientInternalId" )
                .aarQrCodeValue( "fakeAARQRCodeValue" )
                .build();

        InternalNotificationQR internalNotificationQR = InternalNotificationQR.builder()
                .aarQRCodeValue( "fakeAARQRCodeValue" )
                .iun( "iun" )
                .recipientType( NotificationRecipientType.PF )
                .recipientInternalId( "recipientInternalId" )
                .build();

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.of( internalNotificationQR ) );

        ResponseCheckAarDto response = svc.getNotificationByQR( requestCheckAarDto );

        Assertions.assertNotNull( response );
        Assertions.assertEquals( "iun", response.getIun() );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationQRFailure() {
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
                .recipientType( NotificationRecipientType.PF )
                .recipientInternalId( "recipientInternalId" )
                .build();

        Mockito.when( notificationQREntityDao.getNotificationByQR( Mockito.anyString() ) ).thenReturn( Optional.of( internalNotificationQR ) );

        Executable todo = () -> svc.getNotificationByQR( requestCheckAarDto );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

}
